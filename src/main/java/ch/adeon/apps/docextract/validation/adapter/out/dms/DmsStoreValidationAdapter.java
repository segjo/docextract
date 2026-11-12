package ch.adeon.apps.docextract.validation.adapter.out.dms;

import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.shared.http.Http1RestClientFactory;
import ch.adeon.apps.docextract.validation.adapter.out.dms.DmsPropertyPayloadMapper.Prepared;
import ch.adeon.apps.docextract.validation.port.DmsStoreValidationPort;
import ch.adeon.apps.docextract.validation.port.StoreValidationRequest;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Read-only check via {@code POST /dms/r/{repositoryId}/storevalidate} (ADR-016): nothing is
 * created and no location is taken. The request is serialised as valid JSON but sent as {@code
 * text/plain}, which d.velop requires. The {@code Origin} header is set because the call is a POST,
 * like for every other POST to the DMS. Neither values, file name nor DMS message are logged.
 */
@Component
public class DmsStoreValidationAdapter implements DmsStoreValidationPort {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final DmsPropertyPayloadMapper mapper;
  private final StoreValidateResponseParser parser;

  public DmsStoreValidationAdapter(
      ObjectMapper objectMapper,
      DmsPropertyPayloadMapper mapper,
      StoreValidateResponseParser parser,
      @Value("${docextract.validation.dms-timeout:PT10S}") Duration timeout) {
    this.restClient = Http1RestClientFactory.create(timeout);
    this.objectMapper = objectMapper;
    this.mapper = mapper;
    this.parser = parser;
  }

  @Override
  public StoreValidationResult validate(
      String tenantId, StoreValidationRequest request, DvelopCredential credential) {
    // The credential's base URI already addresses the tenant's own system.
    Objects.requireNonNull(tenantId, "tenantId");
    Prepared prepared = mapper.toStorePayload(request);
    byte[] body =
        objectMapper.writeValueAsString(prepared.payload()).getBytes(StandardCharsets.UTF_8);
    Answer answer;
    try {
      answer =
          restClient
              .post()
              .uri(
                  credential.baseUri() + "/dms/r/{repositoryId}/storevalidate",
                  request.repositoryId())
              .header(HttpHeaders.ORIGIN, credential.baseUri())
              .header(credential.headerName(), credential.headerValue())
              .headers(headers -> addAcceptLanguage(headers, credential))
              .contentType(MediaType.TEXT_PLAIN)
              .accept(MediaType.APPLICATION_JSON)
              .body(body)
              .exchange((httpRequest, response) ->
                  new Answer(response.getStatusCode().value(), response.bodyTo(String.class)));
    } catch (RestClientException ex) {
      throw new AdapterUnavailableException("storevalidate not reachable", ex);
    }
    return parser.parse(
        answer.status(), answer.body(), request.definitions().keySet(), prepared.submitted());
  }

  private static void addAcceptLanguage(HttpHeaders headers, DvelopCredential credential) {
    if (credential.acceptLanguage() != null) {
      headers.set(HttpHeaders.ACCEPT_LANGUAGE, credential.acceptLanguage());
    }
  }

  private record Answer(int status, String body) {}
}
