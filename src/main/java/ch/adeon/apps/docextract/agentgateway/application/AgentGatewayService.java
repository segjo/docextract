package ch.adeon.apps.docextract.agentgateway.application;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.shared.error.InvalidRequestException;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
/**
 * Agent path (FR-6) on the same read services as REST ({@link GetExtractionResult}, {@link
 * GetSimilarDocuments}). Guardrails enforced here, not in the MCP adapter: argument validation,
 * ownership of the process by the caller's tenant (NfA-4; the read services answer unknown and
 * foreign processes alike with NOT_FOUND) and one audit entry per call with only the argument hash
 * and the actor (C-4).
 */
@Service
public class AgentGatewayService implements McpTool {

  private static final Pattern PROCESS_ID = Pattern.compile(PROCESS_ID_PATTERN);

  private final GetExtractionResult getExtractionResult;
  private final GetSimilarDocuments getSimilarDocuments;
  private final AuthContextPort authContextPort;
  private final AuditPort auditPort;

  public AgentGatewayService(
      GetExtractionResult getExtractionResult,
      GetSimilarDocuments getSimilarDocuments,
      AuthContextPort authContextPort,
      AuditPort auditPort) {
    this.getExtractionResult = getExtractionResult;
    this.getSimilarDocuments = getSimilarDocuments;
    this.authContextPort = authContextPort;
    this.auditPort = auditPort;
  }

  @Override
  public ProcessSuggestions getProcessSuggestions(String processId) {
    return call(GET_PROCESS_SUGGESTIONS, processId, tenantId -> toSuggestions(tenantId, processId));
  }

  @Override
  public List<SimilarDocument> findSimilarDocuments(String processId) {
    return call(
        FIND_SIMILAR_DOCUMENTS,
        processId,
        tenantId ->
            getSimilarDocuments.get(tenantId, processId).stream()
                .map(hit -> new SimilarDocument(hit.documentId(), hit.repositoryId(), hit.score()))
                .toList());
  }

  private <T> T call(String toolName, String processId, Function<String, T> query) {
    AuthContext caller = authContextPort.current();
    String outcome = "error";
    try {
      requireValidProcessId(processId);
      T result = query.apply(caller.tenantId());
      outcome = "ok";
      return result;
    } catch (ProcessNotFoundException ex) {
      outcome = "not_found";
      throw ex;
    } catch (InvalidRequestException ex) {
      outcome = "invalid_argument";
      throw ex;
    } finally {
      String argument = processId == null ? "" : processId;
      auditPort.append(
          AuditEvent.byUser(
              caller.tenantId(),
              AuditChannel.MCP,
              caller.agent(),
              caller.actorHash(),
              "mcp.tool." + toolName + "." + outcome,
              Sha256.hex(argument.getBytes(StandardCharsets.UTF_8))));
    }
  }

  private static void requireValidProcessId(String processId) {
    if (processId == null || !PROCESS_ID.matcher(processId).matches()) {
      throw new InvalidRequestException("processId must be a lowercase UUID");
    }
  }

  private ProcessSuggestions toSuggestions(String tenantId, String processId) {
    ExtractionResult result = getExtractionResult.get(tenantId, processId);
    return new ProcessSuggestions(
        processId,
        result.documentTypeId(),
        result.attributes().stream()
            .map(attribute ->
                new ProcessSuggestions.AttributeSuggestion(
                    attribute.propertyId(),
                    attribute.value(),
                    attribute.values(),
                    attribute.confidence()))
            .toList());
  }
}
