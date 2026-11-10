package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import ch.adeon.apps.docextract.agentgateway.application.McpTool;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.springframework.ai.mcp.customizer.McpSyncServerCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;

/**
 * Registers the DocExtract tools with the Spring AI MCP server (Streamable HTTP under {@code
 * /api/v1/mcp}, behind the same d.velop authentication as REST). Off unless {@code
 * docextract.adapters.mcp.enabled=true}; the same property switches the MCP server itself.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "docextract.adapters.mcp.enabled", havingValue = "true")
public class McpServerConfig {

  @Bean
  public List<SyncToolSpecification> docExtractMcpTools(
      McpTool mcpTool, ObjectMapper objectMapper) {
    return new McpToolAdapter(mcpTool, objectMapper).specifications();
  }

  /**
   * Replaces Spring AI's servlet customizer (hence {@code @Primary}): keeps tool calls on the
   * request thread so the SecurityContext (tenant) is visible, and leaves argument validation to
   * the service, so that schema-invalid calls are audited too instead of being rejected by the SDK.
   * On Spring AI upgrades check whether its servlet customizer sets more than immediateExecution.
   */
  @Bean
  @Primary
  public McpSyncServerCustomizer docExtractMcpServerCustomizer() {
    return spec -> spec.immediateExecution(true).validateToolInputs(false);
  }
}
