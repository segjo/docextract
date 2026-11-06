package ch.adeon.apps.docextract.agentgateway.application;

import ch.adeon.apps.docextract.agentgateway.application.AgentToolException.Code;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Agent path (FR-6) on the same read services as REST ({@link GetExtractionResult}, {@link
 * GetSimilarDocuments}). Guardrails enforced here, not in the MCP adapter: argument validation,
 * ownership of the process by the caller's tenant (NfA-4; unknown and foreign both answer
 * NOT_FOUND) and one audit entry per call with only the argument hash (C-4).
 */
@Service
public class AgentGatewayService implements McpTool {

  private static final Pattern PROCESS_ID = Pattern.compile(PROCESS_ID_PATTERN);

  private final GetExtractionResult getExtractionResult;
  private final GetSimilarDocuments getSimilarDocuments;
  private final ProcessDocumentPort processDocumentPort;
  private final AuthContextPort authContextPort;
  private final AuditPort auditPort;

  public AgentGatewayService(
      GetExtractionResult getExtractionResult,
      GetSimilarDocuments getSimilarDocuments,
      ProcessDocumentPort processDocumentPort,
      AuthContextPort authContextPort,
      AuditPort auditPort) {
    this.getExtractionResult = getExtractionResult;
    this.getSimilarDocuments = getSimilarDocuments;
    this.processDocumentPort = processDocumentPort;
    this.authContextPort = authContextPort;
    this.auditPort = auditPort;
  }

  @Override
  public ProcessSuggestions getProcessSuggestions(String processId) {
    return call(GET_PROCESS_SUGGESTIONS, processId, () -> toSuggestions(processId));
  }

  @Override
  public List<SimilarDocument> findSimilarDocuments(String processId) {
    return call(
        FIND_SIMILAR_DOCUMENTS,
        processId,
        () ->
            getSimilarDocuments.get(processId).stream()
                .map(hit -> new SimilarDocument(hit.documentId(), hit.repositoryId(), hit.score()))
                .toList());
  }

  private <T> T call(String toolName, String processId, Supplier<T> query) {
    String outcome = "error";
    try {
      requireValidProcessId(processId);
      requireOwnedByCaller(processId);
      T result = query.get();
      outcome = "ok";
      return result;
    } catch (AgentToolException ex) {
      outcome = ex.code().name().toLowerCase(Locale.ROOT);
      throw ex;
    } finally {
      String argument = processId == null ? "" : processId;
      auditPort.append(
          new AuditEvent(
              "mcp.tool." + toolName + "." + outcome,
              Sha256.hex(argument.getBytes(StandardCharsets.UTF_8)),
              0));
    }
  }

  private static void requireValidProcessId(String processId) {
    if (processId == null || !PROCESS_ID.matcher(processId).matches()) {
      throw new AgentToolException(Code.INVALID_ARGUMENT, "processId must be a lowercase UUID");
    }
  }

  private void requireOwnedByCaller(String processId) {
    String tenantId = authContextPort.current().tenantId();
    boolean owned =
        processDocumentPort
            .find(processId)
            .filter(document -> tenantId.equals(document.tenantId()))
            .isPresent();
    if (!owned) {
      throw new AgentToolException(Code.NOT_FOUND, "process not found");
    }
  }

  private ProcessSuggestions toSuggestions(String processId) {
    ExtractionResult result = getExtractionResult.get(processId);
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
