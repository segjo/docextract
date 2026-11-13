package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.agentgateway.application.AgentGatewayService;
import ch.adeon.apps.docextract.audit.application.ActorKind;
import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.adapter.in.rest.ExtractionController;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.extraction.application.RetryExtraction;
import ch.adeon.apps.docextract.process.adapter.in.sse.ProcessEventsController;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.security.adapter.in.web.SecurityConfig;
import ch.adeon.apps.docextract.security.adapter.out.session.SessionAuthContextAdapter;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import ch.adeon.apps.docextract.shared.hash.Hmac;
import ch.adeon.apps.docextract.validation.adapter.in.rest.ValidationController;
import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBack;
import ch.adeon.apps.docextract.validation.application.RejectSuggestion;
import ch.adeon.apps.docextract.validation.application.ValidateAttributes;
import ch.adeon.apps.docextract.validation.application.ValidationReport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * Real HTTP stack (Tomcat, SecurityConfig, Spring AI MCP server) without database: the agent group
 * ({@code agents}) may only use the MCP endpoint, every other member may use REST and MCP, and the
 * audit names channel and actor kind of each case (§ 8.6, C-2, T-6).
 */
class McpEndpointSecurityTest {

  private static final String PROCESS_ID = "3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b";
  private static final String AGENT_TOKEN = "Bearer agent-api-key";
  private static final String HUMAN_TOKEN = "Bearer human-key";
  private static final String TENANT = "default-tenant";
  private static final String ACTOR_KEY = "test-actor-key-0123456789-0123456789";

  private static ConfigurableApplicationContext context;
  private static String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeAll
  static void start() {
    // command-line args: default properties would lose against application.yml
    context = run("--docextract.agent.group-id=agents");
    baseUrl =
        "http://localhost:"
            + context.getEnvironment().getProperty("local.server.port")
            + "/adeon-docextract/api/v1";
  }

  @AfterAll
  static void stop() {
    context.close();
  }

  private static ConfigurableApplicationContext run(String... extraArgs) {
    String[] withKey = new String[extraArgs.length + 1];
    withKey[0] = "--docextract.audit.actor-key=" + ACTOR_KEY;
    System.arraycopy(extraArgs, 0, withKey, 1, extraArgs.length);
    return runExactly(withKey);
  }

  private static ConfigurableApplicationContext runExactly(String... extraArgs) {
    String[] args = new String[extraArgs.length + 2];
    args[0] = "--server.port=0";
    args[1] = "--docextract.adapters.mcp.enabled=true";
    System.arraycopy(extraArgs, 0, args, 2, extraArgs.length);
    return new SpringApplicationBuilder(TestApp.class).run(args);
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
  void agent_is_forbidden_to_confirm_and_never_reaches_the_service() throws Exception {
    HttpResponse<String> response =
        send(
            post(
                    "/validation/confirm",
                    "{\"processId\":\"" + PROCESS_ID + "\",\"dmsSourceCategory\":\"DREC\"}")
                .header("Authorization", AGENT_TOKEN)
                .build());

    assertThat(response.statusCode()).isEqualTo(403);
    verifyNoInteractions(context.getBean(ConfirmAndWriteBack.class));
  }

  @Test
  void agent_is_forbidden_to_check_attributes_and_never_reaches_the_service() throws Exception {
    HttpResponse<String> response =
        send(
            post(
                    "/validation/check",
                    "{\"processId\":\"" + PROCESS_ID + "\",\"documentTypeId\":\"AGENT-ATTEMPT\"}")
                .header("Authorization", AGENT_TOKEN)
                .build());

    assertThat(response.statusCode()).isEqualTo(403);
    verify(context.getBean(ValidateAttributes.class), never())
        .validate(any(), any(), eq("AGENT-ATTEMPT"), any(), any());
  }

  @Test
  void human_may_check_attributes_over_rest_and_the_audit_says_ui() throws Exception {
    HttpResponse<String> response =
        send(
            post(
                    "/validation/check",
                    "{\"processId\":\""
                        + PROCESS_ID
                        + "\",\"documentTypeId\":\"DREC\",\"attributes\":[]}")
                .header("Authorization", HUMAN_TOKEN)
                .build());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"valid\":true");
    verify(context.getBean(AuditPort.class))
        .append(
            argThat(event ->
                event.eventType().equals("validation.check.valid")
                    && event.channel() == AuditChannel.UI
                    && event.actorKind() == ActorKind.HUMAN));
  }

  @Test
  void agent_is_forbidden_to_reject_and_never_reaches_the_service() throws Exception {
    HttpResponse<String> response =
        send(
            post("/validation/reject", "{\"processId\":\"agent-attempt\"}")
                .header("Authorization", AGENT_TOKEN)
                .build());

    assertThat(response.statusCode()).isEqualTo(403);
    verify(context.getBean(RejectSuggestion.class), never())
        .reject(argThat(command -> command.processId().equals("agent-attempt")));
  }

  @Test
  void agent_is_forbidden_to_read_the_extraction_result_over_rest() throws Exception {
    HttpResponse<String> response =
        get("/processes/" + PROCESS_ID + "/extraction-result", AGENT_TOKEN);

    assertThat(response.statusCode()).isEqualTo(403);
  }

  @Test
  void agent_is_forbidden_to_subscribe_to_process_events() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/processes/" + PROCESS_ID + "/events"))
            .header("Accept", "text/event-stream")
            .header("Authorization", AGENT_TOKEN)
            .GET()
            .build();

    assertThat(send(request).statusCode()).isEqualTo(403);
  }

  @Test
  void human_may_use_rest_endpoints() throws Exception {
    assertThat(get("/processes/" + PROCESS_ID + "/extraction-result", HUMAN_TOKEN).statusCode())
        .isEqualTo(200);
    assertThat(
            send(post("/validation/reject", "{\"processId\":\"" + PROCESS_ID + "\"}")
                    .header("Authorization", HUMAN_TOKEN)
                    .build())
                .statusCode())
        .isEqualTo(204);
  }

  @Test
  void agent_may_use_the_mcp_endpoint_and_the_audit_says_mcp_agent() throws Exception {
    String sessionId = openMcpSession(AGENT_TOKEN);

    HttpResponse<String> call = callGetProcessSuggestions(AGENT_TOKEN, sessionId, PROCESS_ID);

    assertThat(call.statusCode()).isEqualTo(200);
    assertThat(call.body()).doesNotContain("\"isError\":true");
    verify(context.getBean(AuditPort.class))
        .append(
            argThat(event ->
                event.eventType().equals("mcp.tool.get_process_suggestions.ok")
                    && event.channel() == AuditChannel.MCP
                    && event.actorKind() == ActorKind.AGENT
                    && TENANT.equals(event.tenantId())
                    && actorHash("agent-1").equals(event.actorHash())));
  }

  @Test
  void human_may_use_the_mcp_endpoint_and_the_audit_says_mcp_human() throws Exception {
    String sessionId = openMcpSession(HUMAN_TOKEN);

    HttpResponse<String> call = callGetProcessSuggestions(HUMAN_TOKEN, sessionId, PROCESS_ID);

    assertThat(call.statusCode()).isEqualTo(200);
    verify(context.getBean(AuditPort.class))
        .append(
            argThat(event ->
                event.eventType().equals("mcp.tool.get_process_suggestions.ok")
                    && event.channel() == AuditChannel.MCP
                    && event.actorKind() == ActorKind.HUMAN
                    && actorHash("human-1").equals(event.actorHash())));
  }

  @Test
  void schema_invalid_tool_call_reaches_service_and_is_audited() throws Exception {
    String sessionId = openMcpSession(AGENT_TOKEN);

    HttpResponse<String> call = callGetProcessSuggestions(AGENT_TOKEN, sessionId, "x");

    assertThat(call.statusCode()).isEqualTo(200);
    assertThat(call.body()).contains("invalid-request").contains("\"isError\":true");
    verify(context.getBean(AuditPort.class))
        .append(
            argThat(event ->
                event.eventType().equals("mcp.tool.get_process_suggestions.invalid_argument")));
  }

  @Test
  void startup_aborts_without_an_actor_key_outside_profile_dev() {
    assertThatThrownBy(() -> runExactly("--docextract.agent.group-id=agents"))
        .hasStackTraceContaining("docextract.audit.actor-key must be set");
  }

  @Test
  void startup_aborts_with_an_actor_key_shorter_than_32_characters() {
    assertThatThrownBy(() ->
            runExactly("--docextract.agent.group-id=agents", "--docextract.audit.actor-key=short"))
        .hasStackTraceContaining("at least 32 characters");
  }

  @Test
  void profile_dev_starts_without_an_actor_key() {
    try (ConfigurableApplicationContext dev =
        runExactly("--docextract.agent.group-id=agents", "--spring.profiles.active=dev")) {
      assertThat(dev.isRunning()).isTrue();
    }
  }

  @Test
  void enabled_mcp_without_an_agent_group_aborts_the_startup() {
    assertThatThrownBy(() -> run("--docextract.agent.group-id="))
        .hasStackTraceContaining("docextract.agent.group-id");
  }

  @Test
  void explicit_none_starts_without_an_agent_group() throws Exception {
    try (ConfigurableApplicationContext noAgents = run("--docextract.agent.group-id=none")) {
      String url =
          "http://localhost:"
              + noAgents.getEnvironment().getProperty("local.server.port")
              + "/adeon-docextract/api/v1/processes/"
              + PROCESS_ID
              + "/extraction-result";
      // nobody is an agent: the member of the group named "agents" may use REST like anyone
      HttpResponse<String> response =
          http.send(
              HttpRequest.newBuilder(URI.create(url))
                  .header("Authorization", AGENT_TOKEN)
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());

      assertThat(response.statusCode()).isEqualTo(200);
    }
  }

  private static String actorHash(String userId) {
    return Hmac.sha256Hex(ACTOR_KEY.getBytes(StandardCharsets.UTF_8), TENANT + "\n" + userId);
  }

  private String openMcpSession(String token) throws Exception {
    HttpResponse<String> init =
        send(post("/mcp", initialize()).header("Authorization", token).build());
    assertThat(init.statusCode()).isEqualTo(200);
    String sessionId = init.headers().firstValue("Mcp-Session-Id").orElseThrow();
    send(
        post("/mcp", "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
            .header("Authorization", token)
            .header("Mcp-Session-Id", sessionId)
            .build());
    return sessionId;
  }

  private HttpResponse<String> callGetProcessSuggestions(
      String token, String sessionId, String processId) throws Exception {
    return send(
        post(
                "/mcp",
                """
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{
                  "name":"get_process_suggestions","arguments":{"processId":"%s"}}}
                """
                    .formatted(processId))
            .header("Authorization", token)
            .header("Mcp-Session-Id", sessionId)
            .build());
  }

  private HttpRequest.Builder post(String path, String body) {
    return HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString(body));
  }

  private HttpResponse<String> get(String path, String token) throws Exception {
    return send(
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Authorization", token)
            .GET()
            .build());
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
    SessionAuthContextAdapter.class,
    McpServerConfig.class,
    AgentGatewayService.class,
    ProcessEventsController.class,
    ExtractionController.class,
    ValidationController.class
  })
  static class TestApp {

    @Bean
    IdentityProviderPort identityProviderPort() {
      IdentityProviderPort port = mock(IdentityProviderPort.class);
      when(port.validateBearerToken(any(), org.mockito.ArgumentMatchers.eq("agent-api-key")))
          .thenReturn(new DvelopUser("agent-1", "docextract-agent", "Agent", List.of("agents")));
      when(port.validateBearerToken(any(), org.mockito.ArgumentMatchers.eq("human-key")))
          .thenReturn(new DvelopUser("human-1", "user", "Human", List.of()));
      return port;
    }

    @Bean
    AppConfigPort appConfigPort() {
      AppConfigPort port = mock(AppConfigPort.class);
      when(port.systemBaseUri()).thenReturn("http://dms.local");
      return port;
    }

    @Bean
    AuditPort auditPort() {
      return mock(AuditPort.class);
    }

    @Bean
    GetExtractionResult getExtractionResult() {
      GetExtractionResult port = mock(GetExtractionResult.class);
      when(port.get(any(), any())).thenReturn(ExtractionResult.EMPTY);
      return port;
    }

    @Bean
    GetSimilarDocuments getSimilarDocuments() {
      return mock(GetSimilarDocuments.class);
    }

    @Bean
    ProcessEventPort processEventPort() {
      return mock(ProcessEventPort.class);
    }

    @Bean
    RetryExtraction retryExtraction() {
      return mock(RetryExtraction.class);
    }

    @Bean
    OutboundCredentialPort outboundCredentialPort() {
      OutboundCredentialPort port = mock(OutboundCredentialPort.class);
      when(port.current())
          .thenReturn(new DvelopCredential("Authorization", "Bearer t", null, "http://dms.local"));
      return port;
    }

    @Bean
    ConfirmAndWriteBack confirmAndWriteBack() {
      return mock(ConfirmAndWriteBack.class);
    }

    @Bean
    RejectSuggestion rejectSuggestion() {
      return mock(RejectSuggestion.class);
    }

    @Bean
    ValidateAttributes validateAttributes() {
      ValidateAttributes port = mock(ValidateAttributes.class);
      when(port.validate(any(), any(), any(), any(), any()))
          .thenReturn(ValidationReport.valid(List.of()));
      return port;
    }
  }
}
