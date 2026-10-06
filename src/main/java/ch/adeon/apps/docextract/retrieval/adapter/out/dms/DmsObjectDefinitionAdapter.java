package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

import ch.adeon.apps.docextract.retrieval.port.DmsObjectDefinitionPort;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentType;
import ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDefinition;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads a repository's possible document type categories and properties via {@code GET
 * /dms/r/{repositoryId}/objdef} (SPEC §3, dvelop-dmsapp.yaml operationId {@code
 * getDmsObjectDefinitions}) — live per call, same as {@link DmsObjectMetadataAdapter}, never
 * cached, so the requesting user's own session/ACL applies. Each object definition is reduced to
 * the same {@link DmsDocumentMetadata} shape used for a single hit's live properties, so both can
 * be handed to the next processing step interchangeably (SPEC §3): the definitions become the
 * {@link DmsDocumentType#properties()}, {@code contentLanguage} comes from the response's {@code
 * Content-Language} header (same as {@link DmsObjectMetadataAdapter}) and {@code properties} (the
 * actual values) stays empty — objdef has no document instance to read values from, only the type's
 * schema.
 */
@Component
public class DmsObjectDefinitionAdapter implements DmsObjectDefinitionPort {

  private final RestClient restClient;
  private final AppConfigPort appConfigPort;
  private final ObjectMapper objectMapper;

  public DmsObjectDefinitionAdapter(AppConfigPort appConfigPort, ObjectMapper objectMapper) {
    this.restClient = RestClient.create();
    this.appConfigPort = appConfigPort;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<DmsDocumentMetadata> fetchAll(String repositoryId, DvelopCredential credential) {
    try {
      ResponseEntity<String> response =
          restClient
              .get()
              .uri(appConfigPort.systemBaseUri() + "/dms/r/{repositoryId}/objdef", repositoryId)
              .header(credential.headerName(), credential.headerValue())
              .headers(headers -> addAcceptLanguage(headers, credential))
              .retrieve()
              .toEntity(String.class);
      RawResponse raw = objectMapper.readValue(response.getBody(), RawResponse.class);
      String contentLanguage = response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE);
      return raw.objectDefinitions() == null
          ? List.of()
          : raw.objectDefinitions().stream()
              .map(objectDefinition -> toDomain(objectDefinition, contentLanguage))
              .toList();
    } catch (RestClientException ex) {
      throw new DmsObjectDefinitionException(
          "Fetching DMS object definitions failed for repositoryId " + repositoryId, ex);
    } catch (RuntimeException ex) {
      throw new DmsObjectDefinitionException(
          "Parsing DMS object definitions failed for repositoryId " + repositoryId, ex);
    }
  }

  private static void addAcceptLanguage(HttpHeaders headers, DvelopCredential credential) {
    if (credential.acceptLanguage() != null) {
      headers.set(HttpHeaders.ACCEPT_LANGUAGE, credential.acceptLanguage());
    }
  }

  private static DmsDocumentMetadata toDomain(RawObjectDefinition raw, String contentLanguage) {
    List<DmsPropertyDefinition> propertyDefinitions =
        raw.propertyFields() == null
            ? List.of()
            : raw.propertyFields().stream()
                .map(DmsObjectDefinitionAdapter::toPropertyDefinition)
                .toList();
    DmsDocumentType documentType =
        new DmsDocumentType(raw.id(), raw.displayName(), propertyDefinitions);
    return new DmsDocumentMetadata(documentType, contentLanguage, List.of());
  }

  private static DmsPropertyDefinition toPropertyDefinition(RawPropertyField raw) {
    return new DmsPropertyDefinition(
        raw.id(),
        raw.displayName(),
        DmsPropertyDataType.fromCode(raw.dataType()),
        raw.isMandatory(),
        raw.hasValueList(),
        false, // isDynamicValueList: not exposed by objdef
        false, // isHidden: not exposed by objdef
        raw.isModifiable(),
        raw.isSystemProperty(),
        raw.isList(),
        false,
        0,
        0,
        List.of(),
        false);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawResponse(List<RawObjectDefinition> objectDefinitions, int count) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawObjectDefinition(
      String id,
      String uniqueId,
      String displayName,
      boolean writeAccess,
      int objectType,
      List<RawPropertyField> propertyFields) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawPropertyField(
      String id,
      String uniqueId,
      int docFieldId,
      String displayName,
      int dataType,
      boolean isList,
      // Undocumented in dvelop-dmsapp.yaml's ObjDef schema but present on the real/mocked
      // response (see docker/dvelop-mock/mappings/dms_objdef.json).
      boolean isMandatory,
      boolean hasValueList,
      boolean isModifiable,
      boolean isSystemProperty) {}
}
