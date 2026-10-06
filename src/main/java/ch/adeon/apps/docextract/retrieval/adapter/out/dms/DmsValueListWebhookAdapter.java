package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.shared.http.Http1RestClientFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads a single property's valid values via the d.velop valuelist webhook ({@code POST
 * /dms/r/{repositoryId}/validvalues/p/{propertyId}}) — live per call, never cached, so a dependent
 * value list reflects the caller's current knowledge of the document's other property values (SPEC
 * §3). This endpoint is not part of dvelop-dmsapp.yaml; request/response shape is only verified
 * against a captured live example, not the OpenAPI spec.
 */
@Component
public class DmsValueListWebhookAdapter implements ValueListPort {

  private final RestClient restClient;
  private final AppConfigPort appConfigPort;
  private final ObjectMapper objectMapper;
  private final String origin;

  public DmsValueListWebhookAdapter(
      AppConfigPort appConfigPort,
      ObjectMapper objectMapper,
      @Value("${docextract.ingest.dms.origin}") String origin) {
    this.restClient = Http1RestClientFactory.create();
    this.appConfigPort = appConfigPort;
    this.objectMapper = objectMapper;
    this.origin = origin;
  }

  @Override
  public DmsValueList fetchValues(
      String repositoryId,
      String objectDefinitionId,
      String propertyId,
      Map<String, String> extendedProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String searchTerm,
      int maxValues,
      DvelopCredential credential) {
    try {
      Map<String, String> mergedExtendedProperties = new HashMap<>(extendedProperties);
      // Confirmed live: unlike every other d.velop endpoint (string enum Be/Pr/Fr/Ar or
      // Processing/Released/...), this webhook's extendedProperties.property_state is bound
      // server-side as System.Int32 and 400s on our string value. The numeric state codes aren't
      // documented anywhere (this endpoint isn't in dvelop-dmsapp.yaml) and guessing a mapping
      // risks silently wrong filtering, so we drop the key instead of sending a bad value.
      mergedExtendedProperties.remove("property_state");
      String response =
          restClient
              .post()
              .uri(
                  appConfigPort.systemBaseUri()
                      + "/dms/r/{repositoryId}/validvalues/p/{propertyId}",
                  repositoryId,
                  propertyId)
              // Origin is mandatory for write access (POST/PUT/DELETE/PATCH) to prevent CSRF.
              .header(HttpHeaders.ORIGIN, origin)
              .header(credential.headerName(), credential.headerValue())
              .headers(headers -> addAcceptLanguage(headers, credential))
              .body(
                  new RawRequest(
                      1,
                      objectDefinitionId,
                      Map.of(),
                      multivalueExtendedProperties,
                      withSearchTerm(mergedExtendedProperties, propertyId, searchTerm)))
              .retrieve()
              .body(String.class);
      RawResponse raw = objectMapper.readValue(response, RawResponse.class);
      return toDomain(raw, maxValues);
    } catch (RestClientException ex) {
      throw new ValueListWebhookException(
          "Fetching DMS valuelist failed for propertyId " + propertyId, ex);
    } catch (RuntimeException ex) {
      throw new ValueListWebhookException(
          "Parsing DMS valuelist failed for propertyId " + propertyId, ex);
    }
  }

  private static void addAcceptLanguage(HttpHeaders headers, DvelopCredential credential) {
    if (credential.acceptLanguage() != null) {
      headers.set(HttpHeaders.ACCEPT_LANGUAGE, credential.acceptLanguage());
    }
  }

  // Verified live: the webhook filters values starting with the given term when extendedProperties
  // carries the looked-up property's own id mapped to that term (e.g. "15": "Fre" ->
  // "Freigegeben").
  private static Map<String, String> withSearchTerm(
      Map<String, String> extendedProperties, String propertyId, String searchTerm) {
    if (searchTerm == null || searchTerm.isBlank()) {
      return extendedProperties;
    }
    Map<String, String> withSearchTerm = new HashMap<>(extendedProperties);
    withSearchTerm.put(propertyId, searchTerm);
    return withSearchTerm;
  }

  private static DmsValueList toDomain(RawResponse raw, int maxValues) {
    List<String> values =
        raw.values() == null ? List.of() : raw.values().stream().map(RawValue::value).toList();
    if (values.size() <= maxValues) {
      return new DmsValueList(values, false);
    }
    return new DmsValueList(values.subList(0, maxValues), true);
  }

  // Field names/shape match the verified live example (doc/api/dvelop-dmsapp.http); there is no
  // "systemProperties", "docNumber" or "id" field, and documentId is not part of the payload.
  private record RawRequest(
      int type,
      String objectDefinitionId,
      Map<String, String> remarks,
      Map<String, List<String>> multivalueExtendedProperties,
      Map<String, String> extendedProperties) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawResponse(List<RawValue> values) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawValue(String value) {}
}
