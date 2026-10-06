package ch.adeon.apps.docextract.retrieval.adapter.out.dms;

import ch.adeon.apps.docextract.retrieval.port.DmsObjectMetadataPort;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentType;
import ch.adeon.apps.docextract.retrieval.domain.DmsProperty;
import ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDefinition;
import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads a DMS object's properties via {@code GET /dms/r/{repositoryId}/o2/{dmsObjectId}/} (FR-3,
 * SPEC §3) — live per call, never cached, so the requesting user's own session/ACL applies. The raw
 * d.velop response is reduced to a {@link DmsDocumentMetadata} (document type, content language,
 * one {@link DmsProperty} per {@code previewState.previewData.documentProperties} entry) before
 * being handed back to the caller. Every property definition with {@code hasValueList=true} is then
 * additionally enriched with its valid values (capped, {@link ValueListPort}, SPEC §3), using this
 * document's own already-known property values as filter context for dependent value lists.
 */
@Component
public class DmsObjectMetadataAdapter implements DmsObjectMetadataPort {

  private static final Logger log = LoggerFactory.getLogger(DmsObjectMetadataAdapter.class);

  private final RestClient restClient;
  private final AppConfigPort appConfigPort;
  private final ObjectMapper objectMapper;
  private final ValueListPort valueListPort;
  private final int maxValueListValues;

  public DmsObjectMetadataAdapter(
      AppConfigPort appConfigPort,
      ObjectMapper objectMapper,
      ValueListPort valueListPort,
      @Value("${docextract.valuelist.max-values:50}") int maxValueListValues) {
    this.restClient = RestClient.create();
    this.appConfigPort = appConfigPort;
    this.objectMapper = objectMapper;
    this.valueListPort = valueListPort;
    this.maxValueListValues = maxValueListValues;
  }

  @Override
  public String fetchProperties(
      String repositoryId, String documentId, DvelopCredential credential) {
    try {
      ResponseEntity<String> response =
          restClient
              .get()
              .uri(
                  appConfigPort.systemBaseUri() + "/dms/r/{repositoryId}/o2/{documentId}/",
                  repositoryId,
                  documentId)
              .header(credential.headerName(), credential.headerValue())
              .headers(headers -> addAcceptLanguage(headers, credential))
              .retrieve()
              .toEntity(String.class);
      RawResponse raw = objectMapper.readValue(response.getBody(), RawResponse.class);
      String contentLanguage = response.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE);
      DmsDocumentMetadata metadata = toDomain(raw, contentLanguage, repositoryId, credential);
      return objectMapper.writeValueAsString(metadata);
    } catch (RestClientException ex) {
      throw new DmsObjectMetadataException(
          "Fetching DMS object properties failed for documentId " + documentId, ex);
    } catch (RuntimeException ex) {
      throw new DmsObjectMetadataException(
          "Parsing DMS object properties failed for documentId " + documentId, ex);
    }
  }

  private static void addAcceptLanguage(HttpHeaders headers, DvelopCredential credential) {
    if (credential.acceptLanguage() != null) {
      headers.set(HttpHeaders.ACCEPT_LANGUAGE, credential.acceptLanguage());
    }
  }

  private DmsDocumentMetadata toDomain(
      RawResponse raw, String contentLanguage, String repositoryId, DvelopCredential credential) {
    List<RawDocumentProperty> documentProperties = documentProperties(raw.previewState());
    Map<String, List<String>> multivalueValues = multivalueValues(raw.multivalueProperties());
    List<DmsProperty> properties =
        documentProperties.stream()
            .map(property -> toProperty(property, multivalueValues))
            .toList();
    DmsDocumentType documentType =
        enrichWithValueLists(
            documentType(raw, documentProperties),
            documentProperties,
            multivalueValues,
            repositoryId,
            credential);
    return new DmsDocumentMetadata(documentType, contentLanguage, properties);
  }

  private static List<RawDocumentProperty> documentProperties(PreviewState previewState) {
    if (previewState == null
        || previewState.previewData() == null
        || previewState.previewData().documentProperties() == null) {
      return List.of();
    }
    return previewState.previewData().documentProperties();
  }

  // The top-level "category" field is only the display name; the technical id/code is the
  // property_category property's raw value (documentProperties[].value, not displayValue).
  private static DmsDocumentType documentType(
      RawResponse raw, List<RawDocumentProperty> documentProperties) {
    List<DmsPropertyDefinition> propertyDefinitions =
        documentProperties.stream().map(DmsObjectMetadataAdapter::toPropertyDefinition).toList();
    return documentProperties.stream()
        .filter(property -> "property_category".equals(property.id()))
        .findFirst()
        .map(
            property ->
                new DmsDocumentType(property.value(), property.displayValue(), propertyDefinitions))
        .orElseGet(() -> new DmsDocumentType(null, raw.category(), propertyDefinitions));
  }

  /**
   * Best-effort: a failed/empty valuelist lookup for one property must not fail the whole
   * properties fetch, and is expected for a property depending on another one not yet known (SPEC
   * §3) — it just keeps that property's {@code valueList} empty.
   */
  private DmsDocumentType enrichWithValueLists(
      DmsDocumentType documentType,
      List<RawDocumentProperty> documentProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String repositoryId,
      DvelopCredential credential) {
    if (documentType.id() == null) {
      return documentType;
    }
    // documentType's own extended properties (numeric ids), excluding system properties
    // (property_xxx ids, distinguished by definition.isSystemProperty), which the valuelist
    // webhook no longer needs as filter context.
    Map<String, String> extendedProperties = filterContext(documentProperties);
    List<DmsPropertyDefinition> enriched =
        documentType.properties().stream()
            .map(
                definition ->
                    definition.hasValueList()
                        ? withValueList(
                            definition,
                            documentType.id(),
                            extendedProperties,
                            multivalueExtendedProperties,
                            repositoryId,
                            credential)
                        : definition)
            .toList();
    return new DmsDocumentType(documentType.id(), documentType.name(), enriched);
  }

  private static Map<String, String> filterContext(List<RawDocumentProperty> documentProperties) {
    Map<String, String> result = new HashMap<>();
    for (RawDocumentProperty property : documentProperties) {
      boolean isSystemProperty =
          property.definition() != null && property.definition().isSystemProperty();
      if (!isSystemProperty && property.value() != null && !property.value().isBlank()) {
        result.put(property.id(), property.value());
      }
    }
    return result;
  }

  private DmsPropertyDefinition withValueList(
      DmsPropertyDefinition definition,
      String objectDefinitionId,
      Map<String, String> extendedProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String repositoryId,
      DvelopCredential credential) {
    try {
      DmsValueList valueList =
          valueListPort.fetchValues(
              repositoryId,
              objectDefinitionId,
              definition.id(),
              extendedProperties,
              multivalueExtendedProperties,
              null,
              maxValueListValues,
              credential);
      return new DmsPropertyDefinition(
          definition.id(),
          definition.name(),
          definition.dataType(),
          definition.mandatory(),
          definition.hasValueList(),
          definition.isDynamicValueList(),
          definition.isHidden(),
          definition.isModifiable(),
          definition.isSystemProperty(),
          definition.isMultiAttribute(),
          definition.isDocumentTypeProperty(),
          definition.maxRowCount(),
          definition.maxLength(),
          valueList.values(),
          valueList.hasMore());
    } catch (RuntimeException ex) {
      log.warn("failed to fetch DMS valuelist for propertyId={}", definition.id(), ex);
      return definition;
    }
  }

  // documentProperties' own value is truncated for multivalue fields (hasMoreValues=true); the
  // full selected values only appear in the top-level multivalueProperties[].values map.
  private static Map<String, List<String>> multivalueValues(
      List<RawMultivalueProperty> multivalueProperties) {
    if (multivalueProperties == null) {
      return Map.of();
    }
    Map<String, List<String>> result = new HashMap<>();
    for (RawMultivalueProperty property : multivalueProperties) {
      result.put(
          property.id(),
          property.values() == null ? List.of() : List.copyOf(property.values().values()));
    }
    return result;
  }

  private static DmsPropertyDefinition toPropertyDefinition(RawDocumentProperty property) {
    Definition definition = property.definition();
    return new DmsPropertyDefinition(
        property.id(),
        property.name(),
        definition == null
            ? DmsPropertyDataType.UNKNOWN
            : DmsPropertyDataType.fromCode(definition.dataType()),
        definition != null && definition.mandatory(),
        definition != null && definition.hasValueList(),
        definition != null && definition.isDynamicValueList(),
        definition != null && definition.isHidden(),
        definition != null && definition.isModifiable(),
        definition != null && definition.isSystemProperty(),
        definition != null && definition.isMultiAttribute(),
        definition != null && definition.isDocumentTypeProperty(),
        definition == null ? 0 : definition.maxRowCount(),
        definition == null ? 0 : definition.maxLength(),
        List.of(),
        false);
  }

  private static DmsProperty toProperty(
      RawDocumentProperty property, Map<String, List<String>> multivalueValues) {
    boolean multiAttribute =
        property.definition() != null && property.definition().isMultiAttribute();
    return new DmsProperty(
        property.id(),
        multiAttribute ? null : property.displayValue(),
        multiAttribute ? multivalueValues.getOrDefault(property.id(), List.of()) : null);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawResponse(
      String category,
      List<RawMultivalueProperty> multivalueProperties,
      PreviewState previewState) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PreviewState(PreviewData previewData) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PreviewData(List<RawDocumentProperty> documentProperties) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawMultivalueProperty(String id, Map<String, String> values) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RawDocumentProperty(
      String id, String name, String value, String displayValue, Definition definition) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Definition(
      int dataType,
      boolean mandatory,
      boolean hasValueList,
      boolean isDynamicValueList,
      boolean isHidden,
      boolean isModifiable,
      boolean isSystemProperty,
      boolean isMultiAttribute,
      boolean isDocumentTypeProperty,
      int maxRowCount,
      int mappingDirection,
      int maxLength) {}
}
