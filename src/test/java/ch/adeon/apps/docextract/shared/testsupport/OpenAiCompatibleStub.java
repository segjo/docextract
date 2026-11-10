package ch.adeon.apps.docextract.shared.testsupport;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * In-process JDK {@link HttpServer} speaking the OpenAI-compatible wire protocol ({@code POST
 * /chat/completions}, {@code POST /embeddings}) with canned responses, so adapter contract tests
 * and the full-stack pipeline test need neither a model runtime nor network access (ADR-011,
 * ARCHITECTURE § 7 profile {@code ci}). No extra dependency. The chat answer's {@code content} is
 * derived from the raw request body, so one stub can serve different prompts.
 */
public final class OpenAiCompatibleStub implements AutoCloseable {

  /** Matches the {@code embedding.embedding vector(1024)} column (V3). */
  public static final int EMBEDDING_DIMENSIONS = 1024;

  private final HttpServer server;
  private final List<String> chatRequestBodies = new CopyOnWriteArrayList<>();
  private final List<String> embeddingRequestBodies = new CopyOnWriteArrayList<>();

  public OpenAiCompatibleStub(Function<String, String> chatContent) {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
    server.createContext(
        "/",
        exchange -> {
          String requestBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          String path = exchange.getRequestURI().getPath();
          String response;
          if (path.endsWith("/embeddings")) {
            embeddingRequestBodies.add(requestBody);
            response = embeddingResponse();
          } else if (path.endsWith("/chat/completions")) {
            chatRequestBodies.add(requestBody);
            response = chatResponse(chatContent.apply(requestBody));
          } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
          }
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  public String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public List<String> chatRequestBodies() {
    return Collections.unmodifiableList(chatRequestBodies);
  }

  public List<String> embeddingRequestBodies() {
    return Collections.unmodifiableList(embeddingRequestBodies);
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private static String chatResponse(String content) {
    return "{\"id\":\"chatcmpl-stub\",\"object\":\"chat.completion\",\"created\":1,"
        + "\"model\":\"stub-model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\","
        + "\"content\":"
        + jsonString(content)
        + "},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,"
        + "\"completion_tokens\":5,\"total_tokens\":15}}";
  }

  private static String embeddingResponse() {
    String vector =
        IntStream.range(0, EMBEDDING_DIMENSIONS)
            .mapToObj(i -> i == 0 ? "1.0" : "0.0")
            .collect(Collectors.joining(",", "[", "]"));
    return "{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"index\":0,\"embedding\":"
        + vector
        + "}],\"model\":\"stub-embedding\",\"usage\":{\"prompt_tokens\":7,\"total_tokens\":7}}";
  }

  private static String jsonString(String value) {
    return "\""
        + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        + "\"";
  }
}
