package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.domain.LlmTool;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.retrieval.port.DmsValueList;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Read-only LLM tool "lookup_value_list" for one extraction call (ADR-014). The webhook behind
 * {@link ValueListPort} is an HTTP POST but only reads. Guardrails enforced here, not by the model:
 * allowlist of the selected type's writable value-list properties, filters restricted to writable
 * non-system properties, repository/object definition/credential fixed server-side, a call limit
 * per extraction (shared with the other tools, see {@link ToolCallBudget}), and an audit entry per
 * call with only the argument hash (C-4).
 */
final class ValueListLookupTool {

  static final String NAME = "lookup_value_list";

  private static final Logger log = LoggerFactory.getLogger(ValueListLookupTool.class);

  private static final String DESCRIPTION =
      "Reads valid values of a property's DMS value list. Optionally filtered by searchTerm and by"
          + " values of other properties the list depends on. Read-only.";

  private final ValueListPort valueListPort;
  private final AuditPort auditPort;
  private final ObjectMapper objectMapper;
  private final String tenantId;
  private final String repositoryId;
  private final String objectDefinitionId;
  private final DvelopCredential credential;
  private final Set<String> lookupPropertyIds;
  private final Set<String> filterPropertyIds;
  private final ToolCallBudget budget;
  private final int maxValues;

  ValueListLookupTool(
      ValueListPort valueListPort,
      AuditPort auditPort,
      ObjectMapper objectMapper,
      String tenantId,
      String repositoryId,
      String objectDefinitionId,
      DvelopCredential credential,
      List<DmsPropertyDefinition> writableProperties,
      int maxCalls,
      int maxValues) {
    this(
        valueListPort,
        auditPort,
        objectMapper,
        tenantId,
        repositoryId,
        objectDefinitionId,
        credential,
        writableProperties,
        new ToolCallBudget(maxCalls),
        maxValues);
  }

  ValueListLookupTool(
      ValueListPort valueListPort,
      AuditPort auditPort,
      ObjectMapper objectMapper,
      String tenantId,
      String repositoryId,
      String objectDefinitionId,
      DvelopCredential credential,
      List<DmsPropertyDefinition> writableProperties,
      ToolCallBudget budget,
      int maxValues) {
    this.valueListPort = valueListPort;
    this.auditPort = auditPort;
    this.objectMapper = objectMapper;
    this.tenantId = tenantId;
    this.repositoryId = repositoryId;
    this.objectDefinitionId = objectDefinitionId;
    this.credential = credential;
    this.lookupPropertyIds =
        writableProperties.stream()
            .filter(DmsPropertyDefinition::hasValueList)
            .map(DmsPropertyDefinition::id)
            .collect(Collectors.toUnmodifiableSet());
    this.filterPropertyIds =
        writableProperties.stream()
            .filter(property -> !property.isSystemProperty())
            .map(DmsPropertyDefinition::id)
            .collect(Collectors.toUnmodifiableSet());
    this.budget = budget;
    this.maxValues = maxValues;
  }

  boolean isApplicable() {
    return !lookupPropertyIds.isEmpty() && budget.max() > 0;
  }

  LlmTool asLlmTool() {
    return new LlmTool(NAME, DESCRIPTION, inputSchema(), this::call);
  }

  String call(String argumentsJson) {
    String raw = argumentsJson == null ? "" : argumentsJson;
    auditPort.append(
        AuditEvent.system(
            tenantId, "extraction.tool." + NAME, Sha256.hex(raw.getBytes(StandardCharsets.UTF_8))));
    if (!budget.tryAcquire()) {
      return error("tool call limit reached");
    }
    Arguments arguments;
    try {
      arguments = objectMapper.readValue(raw, Arguments.class);
    } catch (RuntimeException ex) {
      return error("invalid arguments");
    }
    if (arguments.propertyId() == null || !lookupPropertyIds.contains(arguments.propertyId())) {
      return error("propertyId not allowed");
    }
    Map<String, String> filters = new HashMap<>();
    if (arguments.filters() != null) {
      for (Filter filter : arguments.filters()) {
        if (filter != null
            && filterPropertyIds.contains(filter.propertyId())
            && !filter.propertyId().equals(arguments.propertyId())
            && filter.value() != null
            && !filter.value().isBlank()) {
          filters.put(filter.propertyId(), filter.value());
        }
      }
    }
    try {
      DmsValueList list =
          valueListPort.fetchValues(
              repositoryId,
              objectDefinitionId,
              arguments.propertyId(),
              filters,
              Map.of(),
              arguments.searchTerm(),
              maxValues,
              credential);
      return objectMapper.writeValueAsString(new Result(list.values(), list.hasMore()));
    } catch (RuntimeException ex) {
      log.warn("value list tool call failed propertyId={}", arguments.propertyId(), ex);
      return error("value list lookup failed");
    }
  }

  private String error(String message) {
    return objectMapper.writeValueAsString(Map.of("error", message));
  }

  private Map<String, Object> inputSchema() {
    Map<String, Object> filterItem = new LinkedHashMap<>();
    filterItem.put("type", "object");
    filterItem.put(
        "properties",
        Map.of(
            "propertyId", Map.of("type", "string", "enum", List.copyOf(filterPropertyIds)),
            "value", Map.of("type", "string")));
    filterItem.put("required", List.of("propertyId", "value"));
    filterItem.put("additionalProperties", false);

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("propertyId", Map.of("type", "string", "enum", List.copyOf(lookupPropertyIds)));
    properties.put("searchTerm", Map.of("type", List.of("string", "null")));
    properties.put("filters", Map.of("type", List.of("array", "null"), "items", filterItem));

    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("required", List.of("propertyId", "searchTerm", "filters"));
    schema.put("additionalProperties", false);
    return schema;
  }

  private record Arguments(String propertyId, String searchTerm, List<Filter> filters) {}

  private record Filter(String propertyId, String value) {}

  private record Result(List<String> values, boolean hasMore) {}
}
