package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import ch.adeon.apps.docextract.agentgateway.application.McpTool;
import ch.adeon.apps.docextract.shared.error.DocextractException;
import ch.adeon.apps.docextract.shared.error.ProblemCode;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
/**
 * Maps the read-only {@link McpTool} port onto MCP tool specifications (Spring AI MCP server,
 * ADR-009). No guardrail logic here: validation, tenant ownership and audit live in the service.
 */
public class McpToolAdapter {

  private static final Logger log = LoggerFactory.getLogger(McpToolAdapter.class);

  private static final String PROCESS_ID = "processId";

  private static final String READ_ONLY_NOTE =
      " Read-only. No tool exists to confirm suggestions, promote documents or write to the DMS:"
          + " that requires explicit approval by a human in the DocExtract UI.";

  private static final Map<String, Object> INPUT_SCHEMA =
      Map.of(
          "type",
          "object",
          "properties",
          Map.of(
              PROCESS_ID,
              Map.of(
                  "type", "string",
                  "format", "uuid",
                  "pattern", McpTool.PROCESS_ID_PATTERN,
                  "description", "processId returned by the DocExtract upload")),
          "required",
          List.of(PROCESS_ID),
          "additionalProperties",
          false);

  private static final ToolAnnotations READ_ONLY =
      ToolAnnotations.builder()
          .readOnlyHint(true)
          .destructiveHint(false)
          .idempotentHint(true)
          .openWorldHint(false)
          .build();

  private final McpTool mcpTool;
  private final ObjectMapper objectMapper;

  public McpToolAdapter(McpTool mcpTool, ObjectMapper objectMapper) {
    this.mcpTool = mcpTool;
    this.objectMapper = objectMapper;
  }

  public List<SyncToolSpecification> specifications() {
    return List.of(
        specification(
            McpTool.GET_PROCESS_SUGGESTIONS,
            "Suggested document type and attribute values (with confidence) of a process of the"
                + " caller's tenant. Empty while extraction has not finished."
                + READ_ONLY_NOTE,
            mcpTool::getProcessSuggestions),
        specification(
            McpTool.FIND_SIMILAR_DOCUMENTS,
            "Top-5 approved documents similar to a process of the caller's tenant"
                + " (documentId, repositoryId, score)."
                + READ_ONLY_NOTE,
            processId -> Map.of("documents", mcpTool.findSimilarDocuments(processId))));
  }

  private SyncToolSpecification specification(
      String name, String description, Function<String, Object> query) {
    Tool tool =
        Tool.builder(name, INPUT_SCHEMA).description(description).annotations(READ_ONLY).build();
    return SyncToolSpecification.builder()
        .tool(tool)
        .callHandler((exchange, request) -> handle(name, query, request.arguments()))
        .build();
  }

  private CallToolResult handle(
      String name, Function<String, Object> query, Map<String, Object> arguments) {
    try {
      Object result = query.apply(processIdOf(arguments));
      return CallToolResult.builder()
          .addTextContent(objectMapper.writeValueAsString(result))
          .isError(false)
          .build();
    } catch (DocextractException ex) {
      if (ex.code() == ProblemCode.INTERNAL_ERROR || ex.code() == ProblemCode.ADAPTER_UNAVAILABLE) {
        log.warn("MCP tool {} failed: {}", name, ex.getClass().getSimpleName());
      }
      return error(ex.code(), ex.detail(), ex.processId());
    } catch (RuntimeException ex) {
      log.warn("MCP tool {} failed: {}", name, ex.getClass().getSimpleName());
      return error(ProblemCode.INTERNAL_ERROR, "An unexpected error occurred", null);
    }
  }

  /**
   * Null unless the arguments are exactly {@code {"processId": "<string>"}}; the service rejects
   * it.
   */
  private static String processIdOf(Map<String, Object> arguments) {
    if (arguments == null
        || arguments.size() != 1
        || !(arguments.get(PROCESS_ID) instanceof String processId)) {
      return null;
    }
    return processId;
  }

  /** Same {@code code}/{@code type}/{@code title}/{@code detail} as the REST ProblemDetail. */
  private CallToolResult error(ProblemCode code, String detail, String processId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("code", code.slug());
    body.put("type", code.typeUri());
    body.put("title", code.title());
    body.put("detail", detail);
    if (processId != null) {
      body.put(PROCESS_ID, processId);
    }
    return CallToolResult.builder()
        .addTextContent(objectMapper.writeValueAsString(body))
        .isError(true)
        .build();
  }
}
