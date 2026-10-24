package ch.adeon.apps.docextract.validation.adapter.out.dms;

import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Stores the confirmed attributes as a new DMS object via {@code POST /r/{repositoryId}/o2m/}
 * (operationId {@code storeDmsObjectWithMapping}, doc/api/dvelop-dmsapp.http {@code
 * storeDocument}). The default mapping source ({@code /dms/r/{repositoryId}/source}) is used so the
 * property keys already present in {@code attributes} (d.3 property ids) are applied directly
 * without a tenant-specific translation. On success the new DMS object's id is parsed out of the
 * {@code Location} response header (e.g. {@code .../o2m/T000004242?sourceid=...} -> {@code
 * T000004242}); any failure or a missing {@code Location} is reported as {@code null} so the caller
 * (ConfirmAndWriteBackService) can treat it as a rejected write-back.
 */
@Component
public class DmsWriteAdapter implements DmsWritePort {

  private static final Logger LOG = LoggerFactory.getLogger(DmsWriteAdapter.class);

  private final RestClient restClient;
  private final AppConfigPort appConfigPort;

  public DmsWriteAdapter(AppConfigPort appConfigPort) {
    this.restClient = RestClient.create();
    this.appConfigPort = appConfigPort;
  }

  @Override
  public String writeAttributes(
      String repositoryId,
      String contentLocationUri,
      Map<String, Object> attributes,
      String filename,
      String sourceCategory,
      DvelopCredential credential) {
    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(appConfigPort.systemBaseUri() + "/dms/r/{repositoryId}/o2m/", repositoryId)
              // Origin is mandatory for write access (POST/PUT/DELETE/PATCH) to prevent CSRF.
              .header(HttpHeaders.ORIGIN, appConfigPort.systemBaseUri())
              .header(credential.headerName(), credential.headerValue())
              .headers(headers -> addAcceptLanguage(headers, credential))
              .contentType(MediaType.valueOf("application/hal+json"))
              .accept(MediaType.APPLICATION_JSON)
              .body(
                  new StoreDocumentRequest(
                      filename,
                      sourceCategory,
                      "/dms/r/" + repositoryId + "/source",
                      contentLocationUri,
                      new SourceProperties(toProperties(attributes))))
              .retrieve()
              .toBodilessEntity();
      String documentId = extractDocumentId(response.getHeaders().getFirst(HttpHeaders.LOCATION));
      if (documentId == null) {
        LOG.warn("d.velop DMS did not return a usable Location header for the stored document");
      }
      return documentId;
    } catch (RestClientException ex) {
      LOG.warn("Storing the document in the d.velop DMS failed", ex);
      return null;
    }
  }

  private static List<SourceProperty> toProperties(Map<String, Object> attributes) {
    List<SourceProperty> properties = new ArrayList<>();
    attributes.forEach((key, value) -> properties.add(new SourceProperty(key, toValues(value))));
    return properties;
  }

  private static List<String> toValues(Object value) {
    if (value instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of(String.valueOf(value));
  }

  // The Location header is of the form
  // "/dms/r/{repositoryId}/o2m/{documentId}?sourceid=..." — the documentId is the last path
  // segment before the (optional) query string.
  private static String extractDocumentId(String location) {
    if (location == null || location.isBlank()) {
      return null;
    }
    String path = location;
    int queryIndex = path.indexOf('?');
    if (queryIndex >= 0) {
      path = path.substring(0, queryIndex);
    }
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash < 0 || lastSlash == path.length() - 1) {
      return null;
    }
    return path.substring(lastSlash + 1);
  }

  private static void addAcceptLanguage(HttpHeaders headers, DvelopCredential credential) {
    if (credential.acceptLanguage() != null) {
      headers.set(HttpHeaders.ACCEPT_LANGUAGE, credential.acceptLanguage());
    }
  }

  private record StoreDocumentRequest(
      String filename,
      String sourceCategory,
      String sourceId,
      String contentLocationUri,
      SourceProperties sourceProperties) {}

  private record SourceProperties(List<SourceProperty> properties) {}

  private record SourceProperty(String key, List<String> values) {}
}
