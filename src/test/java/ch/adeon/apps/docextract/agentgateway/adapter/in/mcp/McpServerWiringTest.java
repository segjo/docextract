package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.adeon.apps.docextract.agentgateway.application.McpTool;
import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerJsonMapperAutoConfiguration;
import org.springframework.ai.mcp.server.webmvc.autoconfigure.McpServerStreamableHttpWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Boot-free check of the docextract.adapters.mcp.enabled switch (default false). */
class McpServerWiringTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  McpServerJsonMapperAutoConfiguration.class,
                  McpServerAutoConfiguration.class,
                  McpServerStreamableHttpWebMvcAutoConfiguration.class))
          .withUserConfiguration(McpServerConfig.class)
          .withBean(McpTool.class, () -> mock(McpTool.class))
          .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
          .withPropertyValues(
              "spring.ai.mcp.server.enabled=${docextract.adapters.mcp.enabled}",
              "spring.ai.mcp.server.protocol=streamable",
              "spring.ai.mcp.server.streamable-http.mcp-endpoint=/api/v1/mcp");

  @Test
  void mcp_server_and_tools_are_off_when_disabled() {
    runner
        .withPropertyValues("docextract.adapters.mcp.enabled=false")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(McpSyncServer.class);
          assertThat(context).doesNotHaveBean("docExtractMcpTools");
        });
  }

  @Test
  void mcp_server_registers_docextract_tools_when_enabled() {
    runner
        .withPropertyValues("docextract.adapters.mcp.enabled=true")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(McpSyncServer.class);
          assertThat(context).hasBean("docExtractMcpTools");
        });
  }
}
