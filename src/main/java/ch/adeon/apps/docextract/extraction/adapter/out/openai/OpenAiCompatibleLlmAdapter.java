package ch.adeon.apps.docextract.extraction.adapter.out.openai;

import ch.adeon.apps.docextract.extraction.application.LlmException;
import ch.adeon.apps.docextract.extraction.domain.LlmCapabilities;
import ch.adeon.apps.docextract.extraction.domain.LlmCapabilities.InputKind;
import ch.adeon.apps.docextract.extraction.domain.LlmCapabilities.StructuredOutputSupport;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import ch.adeon.apps.docextract.extraction.domain.LlmTool;
import ch.adeon.apps.docextract.extraction.domain.ModelInfo;
import ch.adeon.apps.docextract.extraction.port.LlmPort;
import com.openai.azure.AzureOpenAIServiceVersion;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Reference {@link LlmPort} adapter (ADR-011): the widely supported OpenAI-compatible chat
 * completions API, reachable both from a locally hosted runtime (e.g. Ollama's {@code /v1}
 * endpoint, llama.cpp server, vLLM) and from a hosted external provider — same adapter, only
 * configuration differs (base URL, API key). Schema-constrained decoding (T-1) uses the {@code
 * response_format} JSON-schema mechanism; server-side validation is always performed by the caller
 * regardless of what this adapter declares in {@link #capabilities()}. {@code api-version} is an
 * optional escape hatch for Microsoft Foundry/Azure OpenAI-flavoured endpoints; {@code temperature}
 * and {@code top-p} are optional sampling params, left unset (provider default) if blank (still
 * plain Spring AI {@code OpenAiChatOptions} fields, no product-specific type in the core, ADR-009).
 */
@Component
public class OpenAiCompatibleLlmAdapter implements LlmPort {

  /** Safety net against a model that keeps calling tools (T-4); tools enforce their own limit. */
  private static final int MAX_TOOL_ROUNDS = 20;

  private final OpenAiChatModel chatModel;
  private final ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
  private final String modelId;
  private final ObjectMapper objectMapper;

  public OpenAiCompatibleLlmAdapter(
      ObjectMapper objectMapper,
      @Value("${docextract.adapters.llm.base-url}") String baseUrl,
      @Value("${docextract.adapters.llm.api-key:}") String apiKey,
      @Value("${docextract.adapters.llm.model-id}") String modelId,
      @Value("${docextract.adapters.llm.api-version:}") String apiVersion,
      @Value("${docextract.adapters.llm.temperature:}") String temperature,
      @Value("${docextract.adapters.llm.top-p:}") String topP) {
    this.objectMapper = objectMapper;
    this.modelId = modelId;
    OpenAiChatOptions.Builder optionsBuilder =
        OpenAiChatOptions.builder().baseUrl(baseUrl).apiKey(apiKey).model(modelId);
    if (StringUtils.hasText(apiVersion)) {
      optionsBuilder
          .azure(true)
          .azureOpenAIServiceVersion(AzureOpenAIServiceVersion.fromString(apiVersion));
    }
    if (StringUtils.hasText(temperature)) {
      optionsBuilder.temperature(Double.valueOf(temperature));
    }
    if (StringUtils.hasText(topP)) {
      optionsBuilder.topP(Double.valueOf(topP));
    }
    this.chatModel = OpenAiChatModel.builder().options(optionsBuilder.build()).build();
  }

  @Override
  public LlmResponse generate(
      String systemPrompt, String userPrompt, Map<String, Object> jsonSchema, List<LlmTool> tools) {
    try {
      String schema = objectMapper.writeValueAsString(jsonSchema);
      List<ToolCallback> toolCallbacks = tools.stream().map(this::toToolCallback).toList();
      OpenAiChatOptions options =
          OpenAiChatOptions.builder()
              // Explicit .model(...): an unset model field on a per-call options object falls
              // back to a built-in static default during merging, overriding the bean's
              // configured default options.
              .model(modelId)
              .responseFormat(
                  OpenAiChatModel.ResponseFormat.builder()
                      .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
                      .jsonSchema(schema)
                      .build())
              .toolCallbacks(toolCallbacks)
              .build();
      Prompt prompt =
          new Prompt(
              List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)), options);
      ChatResponse response = chatModel.call(prompt);
      int promptTokenCount = promptTokens(response);
      // Spring AI 2.0's ChatModel only returns tool calls; executing them is the caller's job.
      int rounds = 0;
      while (response.hasToolCalls()) {
        if (++rounds > MAX_TOOL_ROUNDS) {
          throw new LlmException("too many tool call rounds for model " + modelId);
        }
        ToolExecutionResult executed = toolCallingManager.executeToolCalls(prompt, response);
        prompt = new Prompt(executed.conversationHistory(), options);
        response = chatModel.call(prompt);
        promptTokenCount += promptTokens(response);
      }
      var result = response.getResult();
      if (result == null) {
        throw new LlmException("no result returned for model " + modelId);
      }
      String content = result.getOutput().getText();
      if (content == null || content.isBlank()) {
        throw new LlmException("no content returned for model " + modelId);
      }
      return new LlmResponse(
          content, new ModelInfo("openai-compatible", modelId), promptTokenCount);
    } catch (LlmException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new LlmException("Chat call failed: " + ex.getMessage(), ex);
    }
  }

  @Override
  public LlmCapabilities capabilities() {
    return new LlmCapabilities(StructuredOutputSupport.NATIVE, InputKind.TEXT);
  }

  private static int promptTokens(ChatResponse response) {
    return response.getMetadata().getUsage().getPromptTokens();
  }

  private ToolCallback toToolCallback(LlmTool tool) {
    ToolDefinition definition =
        ToolDefinition.builder()
            .name(tool.name())
            .description(tool.description())
            .inputSchema(objectMapper.writeValueAsString(tool.inputSchema()))
            .build();
    return new ToolCallback() {
      @Override
      public ToolDefinition getToolDefinition() {
        return definition;
      }

      @Override
      public String call(String toolInput) {
        return tool.handler().apply(toolInput);
      }
    };
  }
}
