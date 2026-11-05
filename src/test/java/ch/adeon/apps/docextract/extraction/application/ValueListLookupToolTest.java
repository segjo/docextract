package ch.adeon.apps.docextract.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.domain.DmsPropertyDefinition;
import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class ValueListLookupToolTest {

  private static final DvelopCredential CREDENTIAL =
      new DvelopCredential("Authorization", "Bearer t", "de-CH");

  private final ValueListPort valueListPort = mock(ValueListPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);

  private ValueListLookupTool tool(int maxCalls) {
    return new ValueListLookupTool(
        valueListPort,
        auditPort,
        new ObjectMapper(),
        "repo-1",
        "type-1",
        CREDENTIAL,
        List.of(
            property("city", true, false),
            property("country", false, false),
            property("property_owner", false, true)),
        maxCalls,
        50);
  }

  @Test
  void fetches_allowed_value_list_with_server_side_repository_and_filtered_filters() {
    when(valueListPort.fetchValues(any(), any(), any(), anyMap(), anyMap(), any(), anyInt(), any()))
        .thenReturn(new DmsValueList(List.of("Bern", "Basel"), true));

    String result =
        tool(5)
            .call(
                """
                {"propertyId":"city","searchTerm":"B","filters":[
                  {"propertyId":"country","value":"CH"},
                  {"propertyId":"property_owner","value":"x"},
                  {"propertyId":"unknown","value":"y"}]}
                """);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, String>> filters = ArgumentCaptor.forClass(Map.class);
    verify(valueListPort)
        .fetchValues(
            eq("repo-1"),
            eq("type-1"),
            eq("city"),
            filters.capture(),
            eq(Map.of()),
            eq("B"),
            eq(50),
            eq(CREDENTIAL));
    assertThat(filters.getValue()).containsExactly(Map.entry("country", "CH"));
    assertThat(result).contains("Bern").contains("\"hasMore\":true");
  }

  @Test
  void rejects_property_without_value_list() {
    String result = tool(5).call("{\"propertyId\":\"country\",\"searchTerm\":null}");

    assertThat(result).contains("propertyId not allowed");
    verify(valueListPort, never())
        .fetchValues(any(), any(), any(), anyMap(), anyMap(), any(), anyInt(), any());
  }

  @Test
  void enforces_call_limit_and_audits_every_call_without_raw_arguments() {
    when(valueListPort.fetchValues(any(), any(), any(), anyMap(), anyMap(), any(), anyInt(), any()))
        .thenReturn(DmsValueList.EMPTY);
    ValueListLookupTool tool = tool(1);
    String args = "{\"propertyId\":\"city\",\"searchTerm\":\"Max Muster\"}";

    tool.call(args);
    String second = tool.call(args);

    assertThat(second).contains("tool call limit reached");
    verify(valueListPort, times(1))
        .fetchValues(any(), any(), any(), anyMap(), anyMap(), any(), anyInt(), any());
    ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort, times(2)).append(audit.capture());
    assertThat(audit.getAllValues())
        .allSatisfy(event -> assertThat(event.promptHash()).doesNotContain("Muster").hasSize(64));
  }

  @Test
  void is_not_applicable_without_value_list_properties() {
    ValueListLookupTool tool =
        new ValueListLookupTool(
            valueListPort,
            auditPort,
            new ObjectMapper(),
            "repo-1",
            "type-1",
            CREDENTIAL,
            List.of(property("country", false, false)),
            5,
            50);

    assertThat(tool.isApplicable()).isFalse();
  }

  private static DmsPropertyDefinition property(
      String id, boolean hasValueList, boolean isSystemProperty) {
    return new DmsPropertyDefinition(
        id,
        id,
        DmsPropertyDataType.ALPHANUMERIC,
        false,
        hasValueList,
        false,
        false,
        true,
        isSystemProperty,
        false,
        false,
        0,
        0,
        List.of(),
        false);
  }
}
