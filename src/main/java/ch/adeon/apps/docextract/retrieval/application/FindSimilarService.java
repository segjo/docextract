package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Embeds+stages a process's chunks (in the same run, avoiding a second embedding call — §6.1/8.5)
 * and searches the {@code APPROVED} corpus with a mandatory tenant/ACL pre-filter (ADR-002, FR-3,
 * NfA-4/-6). Hits below the configurable minimum score are dropped before the result is reported.
 * Each remaining hit's DMS object properties are then looked up live and attached (FR-3, SPEC §3),
 * and the hits are narrowed down to the best-matched document type (see {@link
 * #filterToBestDocumentType(List)}), whose {@link DmsDocumentMetadata} is staged as the document
 * type candidate for the next step — no extra DMS call needed, it is read back out of the already-
 * fetched properties. If no similar document was found at all, all repository document types +
 * properties are fetched instead as a fallback candidate set (SPEC §3, see {@link
 * DmsObjectDefinitionPort}) and staged the same way via {@link DocumentTypeCandidatesPort}. Reports
 * {@code RETRIEVED} progress via SSE (ADR-004) and stages the enriched result for the UI (see
 * {@link RetrievalResultPort}).
 */
@Service
public class FindSimilarService implements FindSimilar {

  private static final Logger log = LoggerFactory.getLogger(FindSimilarService.class);

  private final StageEmbeddings stageEmbeddings;
  private final AuthContextPort authContextPort;
  private final VectorSearchPort vectorSearchPort;
  private final ProcessEventPort processEventPort;
  private final RetrievalResultPort retrievalResultPort;
  private final DmsObjectMetadataPort dmsObjectMetadataPort;
  private final DmsObjectDefinitionPort dmsObjectDefinitionPort;
  private final DocumentTypeCandidatesPort documentTypeCandidatesPort;
  private final ObjectMapper objectMapper;
  private final double minScore;
  private final String repositoryId;

  public FindSimilarService(
      StageEmbeddings stageEmbeddings,
      AuthContextPort authContextPort,
      VectorSearchPort vectorSearchPort,
      ProcessEventPort processEventPort,
      RetrievalResultPort retrievalResultPort,
      DmsObjectMetadataPort dmsObjectMetadataPort,
      DmsObjectDefinitionPort dmsObjectDefinitionPort,
      DocumentTypeCandidatesPort documentTypeCandidatesPort,
      ObjectMapper objectMapper,
      @Value("${docextract.retrieval.min-score:0.0}") double minScore,
      @Value("${docextract.ingest.dms.repository-id}") String repositoryId) {
    this.stageEmbeddings = stageEmbeddings;
    this.authContextPort = authContextPort;
    this.vectorSearchPort = vectorSearchPort;
    this.processEventPort = processEventPort;
    this.retrievalResultPort = retrievalResultPort;
    this.dmsObjectMetadataPort = dmsObjectMetadataPort;
    this.dmsObjectDefinitionPort = dmsObjectDefinitionPort;
    this.documentTypeCandidatesPort = documentTypeCandidatesPort;
    this.objectMapper = objectMapper;
    this.minScore = minScore;
    this.repositoryId = repositoryId;
  }

  @Override
  public List<RetrievalResult> find(String processId, int topK, DvelopCredential credential) {
    log.info("retrieval started processId={}", processId);
    processEventPort.publish(
        new ProcessEvent(processId, ProcessStep.RETRIEVED, StepStatus.STARTED, null));
    try {
      List<EmbeddingVector> vectors = stageEmbeddings.stage(processId);
      if (vectors.isEmpty()) {
        log.info("no chunks embedded for processId={}, skipping similarity search", processId);
        retrievalResultPort.store(processId, List.of());
        stageDocumentTypeCandidates(processId, credential, List.of());
        processEventPort.publish(
            new ProcessEvent(processId, ProcessStep.RETRIEVED, StepStatus.COMPLETED, null));
        return List.of();
      }

      EmbeddingVector queryEmbedding = averageOf(vectors);
      AuthContext auth = authContextPort.current();
      List<RetrievalResult> results =
          vectorSearchPort.search(auth.tenantId(), auth.aclRef(), queryEmbedding, topK).stream()
              .filter(result -> result.score() >= minScore)
              .map(result -> withDmsProperties(result, credential))
              .toList();
      List<RetrievalResult> bestTypeResults = filterToBestDocumentType(results);
      log.info(
          "retrieval completed processId={} resultCount={} bestTypeResultCount={}",
          processId,
          results.size(),
          bestTypeResults.size());
      retrievalResultPort.store(processId, bestTypeResults);
      stageDocumentTypeCandidates(processId, credential, bestTypeResults);
      processEventPort.publish(
          new ProcessEvent(processId, ProcessStep.RETRIEVED, StepStatus.COMPLETED, null));
      return bestTypeResults;
    } catch (RuntimeException ex) {
      log.warn("retrieval failed processId={}", processId, ex);
      processEventPort.publish(
          new ProcessEvent(processId, ProcessStep.RETRIEVED, StepStatus.FAILED, null));
      if (ex instanceof RetrievalException retrievalException) {
        throw retrievalException;
      }
      throw new RetrievalException("retrieval failed for process " + processId, ex);
    }
  }

  /**
   * Best-effort enrichment: a failed DMS lookup only drops that hit's properties, it must not fail
   * the whole retrieval (FR-3 is the similarity search, the properties are a display nicety).
   */
  private RetrievalResult withDmsProperties(RetrievalResult result, DvelopCredential credential) {
    if (result.repositoryId() == null
        || result.repositoryId().isBlank()
        || result.documentId() == null
        || result.documentId().isBlank()) {
      return result;
    }
    try {
      String properties =
          dmsObjectMetadataPort.fetchProperties(
              result.repositoryId(), result.documentId(), credential);
      return new RetrievalResult(
          result.repositoryId(), result.documentId(), result.score(), properties);
    } catch (RuntimeException ex) {
      log.warn("failed to fetch DMS object properties for documentId={}", result.documentId(), ex);
      return result;
    }
  }

  /**
   * Groups the hits by their DMS document type (read back from the enriched {@code properties}) and
   * keeps only the group with the highest combined score — summing scores rewards a document type
   * backed by several matching hits over a single very strong outlier, i.e. depends on both count
   * and score as required. Hits whose document type couldn't be determined (no/unparsable
   * properties) are excluded from grouping; if none could be typed, the original list is returned
   * unfiltered rather than discarding everything.
   */
  private List<RetrievalResult> filterToBestDocumentType(List<RetrievalResult> results) {
    Map<String, List<RetrievalResult>> byDocumentType = new LinkedHashMap<>();
    for (RetrievalResult result : results) {
      DmsDocumentMetadata metadata = parseMetadata(result);
      if (metadata != null
          && metadata.documentType() != null
          && metadata.documentType().id() != null) {
        byDocumentType
            .computeIfAbsent(metadata.documentType().id(), id -> new ArrayList<>())
            .add(result);
      }
    }
    if (byDocumentType.isEmpty()) {
      return results;
    }
    return byDocumentType.values().stream()
        .max(Comparator.comparingDouble(FindSimilarService::combinedScore))
        .orElseThrow();
  }

  private static double combinedScore(List<RetrievalResult> group) {
    return group.stream().mapToDouble(RetrievalResult::score).sum();
  }

  private DmsDocumentMetadata parseMetadata(RetrievalResult result) {
    if (result.properties() == null) {
      return null;
    }
    try {
      return objectMapper.readValue(result.properties(), DmsDocumentMetadata.class);
    } catch (RuntimeException ex) {
      log.warn(
          "failed to parse DMS document type from properties for documentId={}",
          result.documentId(),
          ex);
      return null;
    }
  }

  /**
   * Stages the document type candidates for the next processing step (SPEC §3): if similar
   * documents were found, this is just the best-matched type's already-fetched {@link
   * DmsDocumentMetadata} (deduplicated, no extra DMS call). Otherwise all repository document types
   * + properties are fetched as a fallback so extraction still has a schema to constrain against.
   * Best-effort: a failed DMS call is logged and leaves no candidates staged, it must not fail the
   * whole retrieval step.
   */
  private void stageDocumentTypeCandidates(
      String processId, DvelopCredential credential, List<RetrievalResult> bestTypeResults) {
    if (!bestTypeResults.isEmpty()) {
      documentTypeCandidatesPort.store(processId, distinctDocumentTypes(bestTypeResults));
      return;
    }
    try {
      List<DmsDocumentMetadata> candidates =
          dmsObjectDefinitionPort.fetchAll(repositoryId, credential);
      log.info(
          "no similar document found, staged {} document type candidates processId={}",
          candidates.size(),
          processId);
      documentTypeCandidatesPort.store(processId, candidates);
    } catch (RuntimeException ex) {
      log.warn("failed to fetch DMS document type candidates processId={}", processId, ex);
    }
  }

  /** One {@link DmsDocumentMetadata} per distinct document type id found among the hits. */
  private List<DmsDocumentMetadata> distinctDocumentTypes(List<RetrievalResult> results) {
    Map<String, DmsDocumentMetadata> byDocumentTypeId = new LinkedHashMap<>();
    for (RetrievalResult result : results) {
      DmsDocumentMetadata metadata = parseMetadata(result);
      if (metadata != null
          && metadata.documentType() != null
          && metadata.documentType().id() != null) {
        byDocumentTypeId.putIfAbsent(metadata.documentType().id(), metadata);
      }
    }
    return List.copyOf(byDocumentTypeId.values());
  }

  /** Query vector representing the whole document: the mean of its chunk embeddings. */
  private static EmbeddingVector averageOf(List<EmbeddingVector> vectors) {
    int dimensions = vectors.get(0).dimensions();
    float[] sum = new float[dimensions];
    for (EmbeddingVector vector : vectors) {
      float[] values = vector.values();
      for (int i = 0; i < dimensions; i++) {
        sum[i] += values[i];
      }
    }
    for (int i = 0; i < dimensions; i++) {
      sum[i] /= vectors.size();
    }
    return new EmbeddingVector(sum);
  }
}
