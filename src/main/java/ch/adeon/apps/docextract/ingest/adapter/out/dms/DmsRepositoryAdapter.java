package ch.adeon.apps.docextract.ingest.adapter.out.dms;

import ch.adeon.apps.docextract.ingest.application.DmsRepositoryException;
import ch.adeon.apps.docextract.ingest.domain.DmsRepository;
import ch.adeon.apps.docextract.ingest.port.DmsRepositoryPort;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads the repository connections the current user can import into via {@code GET
 * /repo/repositories} (doc/api/dvelop-repoapp.md) — live per call, never cached, so the requesting
 * user's own session/ACL applies (same credential-replay pattern as {@code
 * DmsObjectDefinitionAdapter}).
 */
@Component
public class DmsRepositoryAdapter implements DmsRepositoryPort {

  private final RestClient restClient;
  private final AppConfigPort appConfigPort;
  private final ObjectMapper objectMapper;

  public DmsRepositoryAdapter(AppConfigPort appConfigPort, ObjectMapper objectMapper) {
    this.restClient = RestClient.create();
    this.appConfigPort = appConfigPort;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<DmsRepository> fetchAll(DvelopCredential credential) {
    try {
      String body =
          restClient
              .get()
              .uri(appConfigPort.systemBaseUri() + "/repo/repositories")
              .header(credential.headerName(), credential.headerValue())
              .headers(headers -> addAcceptLanguage(headers, credential))
              .accept(MediaType.valueOf("application/hal+json"))
              .retrieve()
              .body(String.class);
      RawResponse raw = objectMapper.readValue(body, RawResponse.class);
      return raw.repositories() == null
          ? List.of()
          : raw.repositories().stream().map(DmsRepositoryAdapter::toDomain).toList();
    } catch (RestClientException ex) {
      throw new DmsRepositoryException("Fetching d.velop repositories failed", ex);
    } catch (RuntimeException ex) {
      throw new DmsRepositoryException("Parsing d.velop repositories failed", ex);
    }
  }

  private static void addAcceptLanguage(HttpHeaders headers, DvelopCredential credential) {
    if (credential.acceptLanguage() != null) {
      headers.set(HttpHeaders.ACCEPT_LANGUAGE, credential.acceptLanguage());
    }
  }

  private static DmsRepository toDomain(RawRepository raw) {
    return new DmsRepository(
        raw.id(), raw.name(), raw.isDefault(), raw.isAvailable(), raw.isReachable());
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawResponse(List<RawRepository> repositories) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawRepository(
      String id, String name, boolean isDefault, boolean isAvailable, boolean isReachable) {}
}
