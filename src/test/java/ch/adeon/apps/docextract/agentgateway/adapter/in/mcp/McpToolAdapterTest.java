package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.agentgateway.application.AgentGatewayService;
import ch.adeon.apps.docextract.agentgateway.application.McpTool;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.application.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** MCP tool surface against the real {@link AgentGatewayService} with mocked ports. */
class McpToolAdapterTest {

  private static final String PROCESS_ID = "3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b";
  private static final String SECRET_VALUE = "Muster AG";

  private final GetExtractionResult getExtractionResult = mock(GetExtractionResult.class);
  private final GetSimilarDocuments getSimilarDocuments = mock(GetSimilarDocuments.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);

  private Map<String, SyncToolSpecification> tools;

  @BeforeEach
  void setUp() {
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "agent", "Agent", true));
    McpTool service =
        new AgentGatewayService(
            getExtractionResult, getSimilarDocuments, authContextPort, auditPort);
    tools =
        new McpToolAdapter(service, JsonMapper.builder().build())
            .specifications().stream()
                .collect(Collectors.toMap(spec -> spec.tool().name(), spec -> spec));
  }

  @Test
  void exposes_exactly_the_two_read_only_tools_with_strict_input_schema() {
    assertThat(tools).containsOnlyKeys("get_process_suggestions", "find_similar_documents");
    tools.values().stream()
        .map(SyncToolSpecification::tool)
        .forEach(tool -> {
          assertThat(tool.annotations().readOnlyHint()).isTrue();
          assertThat(tool.annotations().destructiveHint()).isFalse();
          assertThat(tool.description()).contains("Read-only").contains("human");
          assertThat(tool.inputSchema())
              .containsEntry("type", "object")
              .containsEntry("required", List.of("processId"))
              .containsEntry("additionalProperties", false);
          assertThat(processIdSchema(tool))
              .containsEntry("type", "string")
              .containsEntry("format", "uuid")
              .containsEntry("pattern", McpTool.PROCESS_ID_PATTERN);
        });
  }

  @Test
  void returns_suggestions_of_own_process() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID))
        .thenReturn(
            new ExtractionResult(
                "DREC", List.of(new ExtractedAttribute("p1", SECRET_VALUE, null, 0.8, "x"))));

    CallToolResult result = call("get_process_suggestions", Map.of("processId", PROCESS_ID));

    assertThat(result.isError()).isFalse();
    assertThat(text(result)).contains("\"documentTypeId\":\"DREC\"").contains("\"confidence\":0.8");
  }

  @Test
  void returns_top_similar_documents_without_metadata() {
    when(getSimilarDocuments.get("tenant-a", PROCESS_ID))
        .thenReturn(List.of(new RetrievalResult("repo-1", "doc-1", 0.95, "{\"secret\":1}")));

    CallToolResult result = call("find_similar_documents", Map.of("processId", PROCESS_ID));

    assertThat(result.isError()).isFalse();
    assertThat(text(result))
        .contains("\"documentId\":\"doc-1\"")
        .contains("\"repositoryId\":\"repo-1\"")
        .doesNotContain("secret");
  }

  @Test
  void foreign_tenant_process_yields_error_without_data() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID))
        .thenThrow(new ProcessNotFoundException(PROCESS_ID));

    CallToolResult result = call("get_process_suggestions", Map.of("processId", PROCESS_ID));

    assertThat(result.isError()).isTrue();
    assertThat(text(result)).contains("\"code\":\"not_found\"").doesNotContain("tenant");
  }

  @Test
  void invalid_arguments_yield_validation_error() {
    List<Map<String, Object>> invalid =
        List.of(
            Map.of(),
            Map.of("processId", "not-a-uuid"),
            Map.of("processId", 42),
            Map.of("processId", PROCESS_ID, "tenantId", "tenant-b"));

    invalid.forEach(arguments -> {
      CallToolResult result = call("find_similar_documents", arguments);
      assertThat(result.isError()).isTrue();
      assertThat(text(result)).contains("\"code\":\"invalid_argument\"");
    });
    verifyNoInteractions(getExtractionResult, getSimilarDocuments);
  }

  @Test
  void every_tool_call_is_audited() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID)).thenReturn(ExtractionResult.EMPTY);

    call("get_process_suggestions", Map.of("processId", PROCESS_ID));
    call("find_similar_documents", Map.of("processId", "bad"));
    call("find_similar_documents", Map.of());

    verify(auditPort, times(3)).append(any(AuditEvent.class));
  }

  private CallToolResult call(String name, Map<String, Object> arguments) {
    CallToolRequest request = CallToolRequest.builder(name).arguments(arguments).build();
    return tools.get(name).callHandler().apply(null, request);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> processIdSchema(Tool tool) {
    return (Map<String, Object>)
        ((Map<String, Object>) tool.inputSchema().get("properties")).get("processId");
  }

  private static String text(CallToolResult result) {
    return ((TextContent) result.content().get(0)).text();
  }
}
