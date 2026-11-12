package ch.adeon.apps.docextract.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.retrieval.port.DmsValueList;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.validation.application.AttributeOrigin;
import ch.adeon.apps.docextract.validation.application.CandidateAttribute;
import ch.adeon.apps.docextract.validation.application.ValidateAttributes;
import ch.adeon.apps.docextract.validation.application.ValidatedAttribute;
import ch.adeon.apps.docextract.validation.application.ValidationReport;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AttributeValidationToolTest {

  private static final DvelopCredential CREDENTIAL =
      new DvelopCredential("Authorization", "Bearer t", null, "https://dms.local");
  private static final String SECRET_VALUE = "CH93 0076 2011 6238 5295 7";
  private static final String SECRET_MESSAGE = "Geheime DMS-Meldung zu Max Muster [0010019]";

  private final ValidateAttributes validateAttributes = mock(ValidateAttributes.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private AttributeValidationTool tool(ToolCallBudget budget, int maxRounds) {
    return new AttributeValidationTool(
        validateAttributes,
        auditPort,
        objectMapper,
        "tenant-a",
        "proc-1",
        "DREC",
        CREDENTIAL,
        List.of(property("1"), property("2")),
        budget,
        maxRounds);
  }

  private AttributeValidationTool tool() {
    return tool(new ToolCallBudget(10), 5);
  }

  private static DmsPropertyDefinition property(String id) {
    return new DmsPropertyDefinition(
        id,
        id,
        DmsPropertyDataType.ALPHANUMERIC,
        false,
        false,
        false,
        false,
        true,
        false,
        false,
        false,
        0,
        0,
        List.of(),
        false);
  }

  private static String args(String propertyId, String value) {
    return "{\"attributes\":[{\"propertyId\":\"%s\",\"value\":\"%s\",\"values\":null}]}"
        .formatted(propertyId, value);
  }

  private List<AuditEvent> audited() {
    ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort, org.mockito.Mockito.atLeastOnce()).append(events.capture());
    return events.getAllValues();
  }

  @Test
  void is_available_as_soon_as_the_type_has_writable_properties() {
    assertThat(tool().isApplicable()).isTrue();
    assertThat(tool().asLlmTool().name()).isEqualTo("validate_attributes");
  }

  @Test
  void is_not_available_without_writable_properties_or_without_rounds_or_calls() {
    AttributeValidationTool none =
        new AttributeValidationTool(
            validateAttributes,
            auditPort,
            objectMapper,
            "tenant-a",
            "proc-1",
            "DREC",
            CREDENTIAL,
            List.of(),
            new ToolCallBudget(10),
            5);

    assertThat(none.isApplicable()).isFalse();
    assertThat(tool(new ToolCallBudget(10), 0).isApplicable()).isFalse();
    assertThat(tool(new ToolCallBudget(0), 5).isApplicable()).isFalse();
  }

  @Test
  void the_schema_only_offers_the_writable_properties() {
    JsonNode schema = objectMapper.valueToTree(tool().asLlmTool().inputSchema());

    assertThat(schema.at("/properties/attributes/items/properties/propertyId/enum").size())
        .isEqualTo(2);
  }

  @Test
  void also_empty_values_are_checked_because_hooks_may_set_mandatory_fields() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.valid(List.of()));

    String result = tool().call("{\"attributes\":[]}");

    assertThat(result).contains("\"valid\":true");
    verify(validateAttributes)
        .validate(eq("tenant-a"), eq("proc-1"), eq("DREC"), eq(List.of()), eq(CREDENTIAL));
  }

  @Test
  void only_allowlisted_properties_reach_the_service() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.valid(List.of()));

    tool()
        .call(
            "{\"attributes\":[{\"propertyId\":\"1\",\"value\":\"a\",\"values\":null},"
                + "{\"propertyId\":\"property_filename\",\"value\":\"b\",\"values\":null},"
                + "{\"propertyId\":\"unknown\",\"value\":\"c\",\"values\":null}]}");

    ArgumentCaptor<List<CandidateAttribute>> sent = ArgumentCaptor.forClass(List.class);
    verify(validateAttributes).validate(any(), any(), any(), sent.capture(), any());
    assertThat(sent.getValue()).containsExactly(new CandidateAttribute("1", "a", null, null));
  }

  @Test
  void valid_returns_the_adopted_properties_and_audits_without_values() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(
            ValidationReport.valid(
                List.of(
                    new ValidatedAttribute("1", SECRET_VALUE, null, AttributeOrigin.LLM, false),
                    new ValidatedAttribute("2", "Hook", null, AttributeOrigin.DMS, true))));

    String result = tool().call(args("1", SECRET_VALUE));

    JsonNode json = objectMapper.readTree(result);
    assertThat(json.get("valid").asBoolean()).isTrue();
    assertThat(json.at("/attributes/0/propertyId").asString()).isEqualTo("1");
    assertThat(json.at("/attributes/0/value").asString()).isEqualTo(SECRET_VALUE);
    assertThat(json.at("/attributes/1/value").asString()).isEqualTo("Hook");
    List<AuditEvent> events = audited();
    assertThat(events).hasSize(1);
    AuditEvent event = events.get(0);
    assertThat(event.eventType()).isEqualTo("extraction.tool.validate_attributes.valid");
    assertThat(event.tenantId()).isEqualTo("tenant-a");
    assertThat(event.channel()).isEqualTo(AuditChannel.SYSTEM);
    assertThat(event.argsHash()).hasSize(64);
    assertThat(event.toString()).doesNotContain(SECRET_VALUE).doesNotContain("Hook");
  }

  @Test
  void invalid_returns_the_error_to_the_model_but_never_audits_the_message() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(
            ValidationReport.invalid(new StoreValidationError("1", "0010019", SECRET_MESSAGE)));

    String result = tool().call(args("1", SECRET_VALUE));

    JsonNode json = objectMapper.readTree(result);
    assertThat(json.get("valid").asBoolean()).isFalse();
    assertThat(json.at("/error/propertyId").asString()).isEqualTo("1");
    assertThat(json.at("/error/dmsCode").asString()).isEqualTo("0010019");
    assertThat(json.at("/error/message").asString()).isEqualTo(SECRET_MESSAGE);
    List<AuditEvent> events = audited();
    assertThat(events)
        .extracting(AuditEvent::eventType)
        .containsExactly("extraction.tool.validate_attributes.invalid");
    assertThat(events.toString())
        .doesNotContain("Muster")
        .doesNotContain("Geheime")
        .doesNotContain(SECRET_VALUE);
  }

  @Test
  void remembers_which_values_the_dms_rejected_per_property() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.invalid(new StoreValidationError("1", "1", "m")))
        .thenReturn(ValidationReport.invalid(new StoreValidationError("1", "1", "m")))
        .thenReturn(ValidationReport.invalid(new StoreValidationError(null, "1", "m")));
    AttributeValidationTool tool = tool();

    tool.call(args("1", "first"));
    tool.call(args("1", "second"));
    tool.call(args("2", "ignored: error without property id"));

    assertThat(tool.rejectedValues())
        .containsOnlyKeys("1")
        .extractingByKey("1")
        .isEqualTo(Set.of(List.of("first"), List.of("second")));
  }

  @Test
  void a_rejected_multi_value_is_remembered_as_a_list() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.invalid(new StoreValidationError("1", "1", "m")));
    AttributeValidationTool tool = tool();

    tool.call("{\"attributes\":[{\"propertyId\":\"1\",\"value\":null,\"values\":[\"a\",\"b\"]}]}");

    assertThat(tool.rejectedValues().get("1")).containsExactly(List.of("a", "b"));
  }

  @Test
  void an_unavailable_dms_is_validation_unavailable() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenThrow(new AdapterUnavailableException("storevalidate answered HTTP 503"));

    String result = tool().call(args("1", "x"));

    assertThat(result).isEqualTo("{\"error\":\"validation_unavailable\"}");
    assertThat(audited())
        .extracting(AuditEvent::eventType)
        .containsExactly("extraction.tool.validate_attributes.unavailable");
  }

  @Test
  void the_validation_round_limit_stops_further_dms_checks() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.valid(List.of()));
    AttributeValidationTool tool = tool(new ToolCallBudget(10), 2);

    tool.call(args("1", "a"));
    tool.call(args("1", "b"));
    String third = tool.call(args("1", "c"));

    assertThat(third).contains("validation round limit reached");
    verify(validateAttributes, times(2)).validate(any(), any(), any(), anyList(), any());
    assertThat(audited()).hasSize(3);
  }

  @Test
  void the_tool_call_limit_is_shared_with_the_value_list_tool() {
    ValueListPort valueListPort = mock(ValueListPort.class);
    when(valueListPort.fetchValues(any(), any(), any(), anyMap(), anyMap(), any(), anyInt(), any()))
        .thenReturn(DmsValueList.EMPTY);
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.valid(List.of()));
    ToolCallBudget budget = new ToolCallBudget(2);
    ValueListLookupTool valueList =
        new ValueListLookupTool(
            valueListPort,
            auditPort,
            objectMapper,
            "tenant-a",
            "repo-1",
            "DREC",
            CREDENTIAL,
            List.of(
                new DmsPropertyDefinition(
                    "1",
                    "1",
                    DmsPropertyDataType.ALPHANUMERIC,
                    false,
                    true,
                    false,
                    false,
                    true,
                    false,
                    false,
                    false,
                    0,
                    0,
                    List.of(),
                    false)),
            budget,
            50);
    AttributeValidationTool validation = tool(budget, 5);

    valueList.call("{\"propertyId\":\"1\",\"searchTerm\":null}");
    validation.call(args("1", "a"));
    String third = validation.call(args("1", "b"));
    String fourth = valueList.call("{\"propertyId\":\"1\",\"searchTerm\":null}");

    assertThat(third).contains("tool call limit reached");
    assertThat(fourth).contains("tool call limit reached");
    verify(validateAttributes, times(1)).validate(any(), any(), any(), anyList(), any());
  }

  @Test
  void invalid_arguments_do_not_reach_the_dms() {
    String result = tool().call("not json");

    assertThat(result).contains("invalid arguments");
    verify(validateAttributes, never()).validate(any(), any(), any(), anyList(), any());
    assertThat(audited())
        .extracting(AuditEvent::eventType)
        .containsExactly("extraction.tool.validate_attributes.rejected");
  }

  @Test
  void every_audit_entry_carries_only_a_hash_of_the_arguments() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.valid(List.of()));

    tool().call(args("1", SECRET_VALUE));

    assertThat(audited())
        .allSatisfy(event -> {
          assertThat(event.argsHash()).matches("[0-9a-f]{64}");
          assertThat(event.tenantId()).isEqualTo("tenant-a");
        });
    assertThat(audited().toString()).doesNotContain(SECRET_VALUE);
  }
}
