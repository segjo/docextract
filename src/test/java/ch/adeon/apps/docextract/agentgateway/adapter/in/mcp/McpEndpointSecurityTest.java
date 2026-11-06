package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.agentgateway.application.AgentGatewayService;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.process.adapter.in.sse.ProcessEventsController;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.security.adapter.in.web.SecurityConfig;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Real HTTP stack (Tomcat, SecurityConfig, Spring AI MCP server) without database: initial requests
 * without token stay 401, and a schema-invalid tool call still reaches the service and is audited.
 */
class McpEndpointSecurityTest {

  private static final String PROCESS_ID = "3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b";
  private static final String TOKEN = "Bearer agent-api-key";

  private static ConfigurableApplicationContext context;
  private static String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeAll
  static void start() {
    // command-line args: default properties would lose against application.yml
    context =
        new SpringApplicationBuilder(TestApp.class)
            .run("--server.port=0", "--docextract.adapters.mcp.enabled=true");
    baseUrl =
        "http://localhost:"
            + context.getEnvironment().getProperty("local.server.port")
            + "/adeon-docextract/api/v1";
  }

  @AfterAll
  static void stop() {
    context.close();
  }

  @Test
  void mcp_endpoint_without_token_is_401() throws Exception {
    HttpResponse<String> response = send(post("/mcp", initialize()).build());

    assertThat(response.statusCode()).isEqualTo(401);
  }

  @Test
  void process_events_without_token_is_401() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/processes/" + PROCESS_ID + "/events"))
            .header("Accept", "text/event-stream")
            .GET()
            .build();

    assertThat(send(request).statusCode()).isEqualTo(401);
    verifyNoInteractions(context.getBean(ProcessEventPort.class));
  }

  @Test
  void schema_invalid_tool_call_reaches_service_and_is_audited() throws Exception {
    HttpResponse<String> init =
        send(post("/mcp", initialize()).header("Authorization", TOKEN).build());
    assertThat(init.statusCode()).isEqualTo(200);
    String sessionId = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
    send(
        post("/mcp", "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
            .header("Authorization", TOKEN)
            .header("Mcp-Session-Id", sessionId)
            .build());

    HttpResponse<String> call =
        send(
            post(
                    "/mcp",
                    """
                    {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{
                      "name":"get_process_suggestions","arguments":{"processId":"x"}}}
                    """)
                .header("Authorization", TOKEN)
                .header("Mcp-Session-Id", sessionId)
                .build());

    assertThat(call.statusCode()).isEqualTo(200);
    assertThat(call.body()).contains("invalid_argument").contains("\"isError\":true");
    verify(context.getBean(AuditPort.class))
        .append(
            argThat(event ->
                event.eventType().equals("mcp.tool.get_process_suggestions.invalid_argument")));
    verify(context.getBean(ProcessDocumentPort.class), never()).find(anyString());
  }

  private HttpRequest.Builder post(String path, String body) {
    return HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString(body));
  }

  private HttpResponse<String> send(HttpRequest request) throws Exception {
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static String initialize() {
    return """
    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
      "capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
    """;
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(
      excludeName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
      })
  @Import({
    SecurityConfig.class,
    McpServerConfig.class,
    AgentGatewayService.class,
    ProcessEventsController.class
  })
  static class TestApp {

    @Bean
    IdentityProviderPort identityProviderPort() {
      IdentityProviderPort port = mock(IdentityProviderPort.class);
      when(port.validateBearerToken("agent-api-key"))
          .thenReturn(new DvelopUser("agent-1", "docextract-agent", "Agent", List.of()));
      return port;
    }

    @Bean
    AuthContextPort authContextPort() {
      AuthContextPort port = mock(AuthContextPort.class);
      when(port.current()).thenReturn(new AuthContext("default-tenant", "agent-1", "Agent"));
      return port;
    }

    @Bean
    AuditPort auditPort() {
      return mock(AuditPort.class);
    }

    @Bean
    ProcessDocumentPort processDocumentPort() {
      return mock(ProcessDocumentPort.class);
    }

    @Bean
    GetExtractionResult getExtractionResult() {
      return mock(GetExtractionResult.class);
    }

    @Bean
    GetSimilarDocuments getSimilarDocuments() {
      return mock(GetSimilarDocuments.class);
    }

    @Bean
    ProcessEventPort processEventPort() {
      return mock(ProcessEventPort.class);
    }
  }
}
