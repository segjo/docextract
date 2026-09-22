package ch.adeon.apps.docextract.shared.health;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Checks reachability of PostgreSQL and the local container dependencies (docling, Gotenberg,
 * Ollama, d.velop jstore). Only used to render an operator-facing status; it never touches document
 * content (C-1) and never triggers a write.
 */
@Service
public class SystemHealthService {

  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  private final JdbcTemplate jdbcTemplate;
  private final String jstoreUri;
  private final RestClient restClient;
  private final String gotenbergBaseUri;
  private final String doclingBaseUri;
  private final String ollamaBaseUri;

  public SystemHealthService(
      JdbcTemplate jdbcTemplate,
      @Value("${docextract.dvelop.jstore-uri}") String jstoreUri,
      @Value("${docextract.gotenberg.base-uri}") String gotenbergBaseUri,
      @Value("${docextract.docling.base-uri}") String doclingBaseUri,
      @Value("${docextract.ollama.base-uri}") String ollamaBaseUri) {
    this.jdbcTemplate = jdbcTemplate;
    this.jstoreUri = jstoreUri;
    this.gotenbergBaseUri = gotenbergBaseUri;
    this.doclingBaseUri = doclingBaseUri;
    this.ollamaBaseUri = ollamaBaseUri;
    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(TIMEOUT);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
  }

  public SystemHealth check() {
    return SystemHealth.of(
        List.of(
            checkComponent("database", this::checkDatabase),
            checkComponent("docling", () -> checkHttp(doclingBaseUri + "/health")),
            checkComponent("gotenberg", () -> checkHttp(gotenbergBaseUri + "/health")),
            checkComponent("ollama", () -> checkHttp(ollamaBaseUri + "/")),
            checkComponent("dvelop-jstore", () -> checkHttp(jstoreUri))));
  }

  private ComponentHealth checkComponent(String name, Runnable probe) {
    try {
      probe.run();
      return ComponentHealth.up(name);
    } catch (Exception ex) {
      return ComponentHealth.down(name, ex.getMessage());
    }
  }

  private void checkDatabase() {
    jdbcTemplate.queryForObject("SELECT 1", Integer.class);
  }

  private void checkHttp(String uri) {
    try {
      restClient.get().uri(uri).retrieve().toBodilessEntity();
    } catch (RestClientException ex) {
      throw new IllegalStateException(uri + ": " + ex.getMessage(), ex);
    }
  }
}
