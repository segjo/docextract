package ch.adeon.apps.docextract.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentType;
import ch.adeon.apps.docextract.retrieval.domain.EmbeddingVector;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalException;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FindSimilarServiceTest {

  private final StageEmbeddings stageEmbeddings = mock(StageEmbeddings.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final VectorSearchPort vectorSearchPort = mock(VectorSearchPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final RetrievalResultPort retrievalResultPort = mock(RetrievalResultPort.class);
  private final DmsObjectMetadataPort dmsObjectMetadataPort = mock(DmsObjectMetadataPort.class);
  private final DmsObjectDefinitionPort dmsObjectDefinitionPort =
      mock(DmsObjectDefinitionPort.class);
  private final DocumentTypeCandidatesPort documentTypeCandidatesPort =
      mock(DocumentTypeCandidatesPort.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DvelopCredential credential =
      new DvelopCredential("Authorization", "Bearer t", "de-CH");
  private final FindSimilarService service =
      new FindSimilarService(
          stageEmbeddings,
          authContextPort,
          vectorSearchPort,
          processEventPort,
          retrievalResultPort,
          dmsObjectMetadataPort,
          dmsObjectDefinitionPort,
          documentTypeCandidatesPort,
          objectMapper,
          0.0,
          "repo-1");

  @Test
  void searches_with_the_mean_of_the_staged_chunk_embeddings_and_the_caller_s_tenant_acl() {
    when(stageEmbeddings.stage("p1"))
        .thenReturn(
            List.of(
                new EmbeddingVector(new float[] {1f, 1f}),
                new EmbeddingVector(new float[] {3f, 1f})));
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "acl-a", "u", "U"));
    RetrievalResult top = new RetrievalResult("repo-1", "doc-1", 0.9, null);
    when(vectorSearchPort.search(eq("tenant-a"), eq("acl-a"), any(), eq(5)))
        .thenReturn(List.of(top));
    when(dmsObjectMetadataPort.fetchProperties("repo-1", "doc-1", credential))
        .thenReturn("{\"id\":\"doc-1\"}");

    List<RetrievalResult> results = service.find("p1", 5, credential);

    RetrievalResult enriched = new RetrievalResult("repo-1", "doc-1", 0.9, "{\"id\":\"doc-1\"}");
    assertThat(results).containsExactly(enriched);
    verify(vectorSearchPort)
        .search("tenant-a", "acl-a", new EmbeddingVector(new float[] {2f, 1f}), 5);
    verify(retrievalResultPort).store("p1", List.of(enriched));
    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.RETRIEVED, StepStatus.STARTED, null));
    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.RETRIEVED, StepStatus.COMPLETED, null));
  }

  @Test
  void skips_the_dms_properties_lookup_when_repository_id_or_document_id_is_blank() {
    when(stageEmbeddings.stage("p1"))
        .thenReturn(List.of(new EmbeddingVector(new float[] {1f, 1f})));
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "acl-a", "u", "U"));
    RetrievalResult blankRepository = new RetrievalResult("", "doc-1", 0.9, null);
    when(vectorSearchPort.search(eq("tenant-a"), eq("acl-a"), any(), eq(5)))
        .thenReturn(List.of(blankRepository));

    List<RetrievalResult> results = service.find("p1", 5, credential);

    assertThat(results).containsExactly(blankRepository);
    verify(dmsObjectMetadataPort, never()).fetchProperties(any(), any(), any());
  }

  @Test
  void keeps_the_result_without_properties_when_the_dms_lookup_fails() {
    when(stageEmbeddings.stage("p1"))
        .thenReturn(List.of(new EmbeddingVector(new float[] {1f, 1f})));
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "acl-a", "u", "U"));
    RetrievalResult top = new RetrievalResult("repo-1", "doc-1", 0.9, null);
    when(vectorSearchPort.search(eq("tenant-a"), eq("acl-a"), any(), eq(5)))
        .thenReturn(List.of(top));
    when(dmsObjectMetadataPort.fetchProperties("repo-1", "doc-1", credential))
        .thenThrow(new RuntimeException("dms unreachable"));

    List<RetrievalResult> results = service.find("p1", 5, credential);

    assertThat(results).containsExactly(top);
  }

  @Test
  void drops_hits_below_the_configured_minimum_score() {
    FindSimilarService thresholded =
        new FindSimilarService(
            stageEmbeddings,
            authContextPort,
            vectorSearchPort,
            processEventPort,
            retrievalResultPort,
            dmsObjectMetadataPort,
            dmsObjectDefinitionPort,
            documentTypeCandidatesPort,
            objectMapper,
            0.8,
            "repo-1");
    when(stageEmbeddings.stage("p1"))
        .thenReturn(List.of(new EmbeddingVector(new float[] {1f, 1f})));
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "acl-a", "u", "U"));
    RetrievalResult strong = new RetrievalResult("repo-1", "doc-1", 0.9, null);
    RetrievalResult weak = new RetrievalResult("repo-1", "doc-2", 0.3, null);
    when(vectorSearchPort.search(eq("tenant-a"), eq("acl-a"), any(), eq(5)))
        .thenReturn(List.of(strong, weak));

    List<RetrievalResult> results = thresholded.find("p1", 5, credential);

    assertThat(results).containsExactly(strong);
    verify(retrievalResultPort).store("p1", List.of(strong));
    verify(dmsObjectMetadataPort, never()).fetchProperties(eq("repo-1"), eq("doc-2"), any());
  }

  @Test
  void skips_the_similarity_search_for_a_document_with_no_embeddable_chunks() {
    when(stageEmbeddings.stage("p1")).thenReturn(List.of());

    List<RetrievalResult> results = service.find("p1", 5, credential);

    assertThat(results).isEmpty();
    verify(vectorSearchPort, never()).search(any(), any(), any(), anyInt());
    verify(retrievalResultPort).store("p1", List.of());
    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.RETRIEVED, StepStatus.COMPLETED, null));
  }

  @Test
  void publishes_a_failed_event_and_wraps_the_error_when_staging_fails() {
    when(stageEmbeddings.stage("p1")).thenThrow(new RetrievalException("boom"));

    assertThatThrownBy(() -> service.find("p1", 5, credential))
        .isInstanceOf(RetrievalException.class);

    verify(processEventPort)
        .publish(new ProcessEvent("p1", ProcessStep.RETRIEVED, StepStatus.FAILED, null));
  }

  @Test
  void keeps_only_the_hits_of_the_document_type_with_the_highest_combined_score() {
    when(stageEmbeddings.stage("p1"))
        .thenReturn(List.of(new EmbeddingVector(new float[] {1f, 1f})));
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "acl-a", "u", "U"));
    RetrievalResult typeAWeak = withProperties("repo-1", "doc-1", 0.91, "DREC");
    RetrievalResult typeAStrong = withProperties("repo-1", "doc-2", 0.95, "DREC");
    RetrievalResult typeBSingle = withProperties("repo-1", "doc-3", 0.99, "DLIEF");
    when(vectorSearchPort.search(eq("tenant-a"), eq("acl-a"), any(), eq(5)))
        .thenReturn(List.of(typeAWeak, typeAStrong, typeBSingle));
    stubDmsProperties(typeAWeak, typeAStrong, typeBSingle);

    List<RetrievalResult> results = service.find("p1", 5, credential);

    // DREC's combined score (0.91+0.95=1.86) beats DLIEF's single 0.99 hit.
    assertThat(results).containsExactlyInAnyOrder(typeAWeak, typeAStrong);
    verify(retrievalResultPort)
        .store(eq("p1"), argThat(list -> list.containsAll(List.of(typeAWeak, typeAStrong))));
    // The candidate is read back from the already-fetched hits, no extra DMS objdef call needed.
    verify(documentTypeCandidatesPort)
        .store(
            eq("p1"),
            argThat(
                candidates ->
                    candidates.size() == 1
                        && "DREC".equals(candidates.get(0).documentType().id())));
    verify(dmsObjectDefinitionPort, never()).fetchAll(any(), any());
  }

  @Test
  void fetches_all_dms_document_type_candidates_when_no_similar_document_is_found() {
    when(stageEmbeddings.stage("p1"))
        .thenReturn(List.of(new EmbeddingVector(new float[] {1f, 1f})));
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "acl-a", "u", "U"));
    when(vectorSearchPort.search(eq("tenant-a"), eq("acl-a"), any(), eq(5))).thenReturn(List.of());
    List<DmsDocumentMetadata> candidates =
        List.of(
            new DmsDocumentMetadata(
                new DmsDocumentType("DREC", "Rechnung", List.of()), null, List.of()));
    when(dmsObjectDefinitionPort.fetchAll("repo-1", credential)).thenReturn(candidates);

    List<RetrievalResult> results = service.find("p1", 5, credential);

    assertThat(results).isEmpty();
    verify(documentTypeCandidatesPort).store("p1", candidates);
  }

  @Test
  void fetches_document_type_candidates_when_the_process_has_no_embeddable_chunks() {
    when(stageEmbeddings.stage("p1")).thenReturn(List.of());
    List<DmsDocumentMetadata> candidates =
        List.of(
            new DmsDocumentMetadata(
                new DmsDocumentType("DREC", "Rechnung", List.of()), null, List.of()));
    when(dmsObjectDefinitionPort.fetchAll("repo-1", credential)).thenReturn(candidates);

    service.find("p1", 5, credential);

    verify(documentTypeCandidatesPort).store("p1", candidates);
  }

  private static RetrievalResult withProperties(
      String repositoryId, String documentId, double score, String documentTypeId) {
    String properties =
        "{\"documentType\":{\"id\":\""
            + documentTypeId
            + "\",\"name\":null,\"properties\":[]},"
            + "\"contentLanguage\":null,\"properties\":[]}";
    return new RetrievalResult(repositoryId, documentId, score, properties);
  }

  /** {@code withDmsProperties} always re-fetches; make it echo back each fixture's properties. */
  private void stubDmsProperties(RetrievalResult... results) {
    for (RetrievalResult result : results) {
      when(dmsObjectMetadataPort.fetchProperties(
              result.repositoryId(), result.documentId(), credential))
          .thenReturn(result.properties());
    }
  }
}
