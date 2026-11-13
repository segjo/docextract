package ch.adeon.apps.docextract.validation.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.adeon.apps.docextract.audit.application.ActorKind;
import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.shared.adapter.web.GlobalExceptionHandler;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.validation.application.AttributeOrigin;
import ch.adeon.apps.docextract.validation.application.CandidateAttribute;
import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBack;
import ch.adeon.apps.docextract.validation.application.RejectSuggestion;
import ch.adeon.apps.docextract.validation.application.ValidateAttributes;
import ch.adeon.apps.docextract.validation.application.ValidatedAttribute;
import ch.adeon.apps.docextract.validation.application.ValidationReport;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

/** POST /api/v1/validation/check (ADR-016): report shape, error mapping and audit. */
class ValidationControllerCheckTest {

  private static final String SECRET_VALUE = "CH93 0076 2011 6238 5295 7";
  private static final String BODY =
      """
      {"processId":"p1","documentTypeId":"DREC","attributes":[
        {"propertyId":"1","value":"%s","values":null,"origin":null},
        {"propertyId":"2","value":null,"values":["a","b"],"origin":"DMS"}]}
      """
          .formatted(SECRET_VALUE);

  private final ValidateAttributes validateAttributes = mock(ValidateAttributes.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AuthContextPort authContextPort = mock(AuthContextPort.class);
    when(authContextPort.current())
        .thenReturn(new AuthContext("tenant-a", "user-1", "User", false, "actor-hash"));
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    when(outboundCredentialPort.current())
        .thenReturn(new DvelopCredential("Authorization", "Bearer t", null, "https://dms.local"));
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ValidationController(
                    mock(ConfirmAndWriteBack.class),
                    mock(RejectSuggestion.class),
                    validateAttributes,
                    authContextPort,
                    outboundCredentialPort,
                    auditPort,
                    new ObjectMapper()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  private org.springframework.test.web.servlet.ResultActions check(String body) throws Exception {
    return mockMvc.perform(
        post("/api/v1/validation/check").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private AuditEvent audited() {
    ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort).append(event.capture());
    return event.getValue();
  }

  @Test
  void valid_returns_the_report_with_the_adopted_attributes() throws Exception {
    when(validateAttributes.validate(any(), any(), any(), any(), any()))
        .thenReturn(
            ValidationReport.valid(
                List.of(
                    new ValidatedAttribute("4", "2026-11-11", null, AttributeOrigin.DMS, true))));

    check(BODY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.attributes[0].propertyId").value("4"))
        .andExpect(jsonPath("$.attributes[0].origin").value("DMS"))
        .andExpect(jsonPath("$.attributes[0].changedByDms").value(true))
        .andExpect(jsonPath("$.error").doesNotExist());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CandidateAttribute>> sent = ArgumentCaptor.forClass(List.class);
    verify(validateAttributes)
        .validate(
            org.mockito.ArgumentMatchers.eq("tenant-a"),
            org.mockito.ArgumentMatchers.eq("p1"),
            org.mockito.ArgumentMatchers.eq("DREC"),
            sent.capture(),
            any());
    assertThat(sent.getValue())
        .containsExactly(
            new CandidateAttribute("1", SECRET_VALUE, null, null),
            new CandidateAttribute("2", null, List.of("a", "b"), AttributeOrigin.DMS));
  }

  @Test
  void invalid_is_still_a_200_with_the_one_error() throws Exception {
    when(validateAttributes.validate(any(), any(), any(), any(), any()))
        .thenReturn(
            ValidationReport.invalid(
                new StoreValidationError("4", "0010019", "Pflichtfeld fehlt")));

    check(BODY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.attributes").doesNotExist())
        .andExpect(jsonPath("$.error.propertyId").value("4"))
        .andExpect(jsonPath("$.error.dmsCode").value("0010019"))
        .andExpect(jsonPath("$.error.message").value("Pflichtfeld fehlt"));
  }

  @Test
  void the_audit_is_channel_ui_and_has_only_a_hash_and_the_result() throws Exception {
    when(validateAttributes.validate(any(), any(), any(), any(), any()))
        .thenReturn(ValidationReport.invalid(new StoreValidationError("4", "1", "Meldung Muster")));

    check(BODY).andExpect(status().isOk());

    AuditEvent event = audited();
    assertThat(event.eventType()).isEqualTo("validation.check.invalid");
    assertThat(event.channel()).isEqualTo(AuditChannel.UI);
    assertThat(event.actorKind()).isEqualTo(ActorKind.HUMAN);
    assertThat(event.tenantId()).isEqualTo("tenant-a");
    assertThat(event.actorHash()).isEqualTo("actor-hash");
    assertThat(event.argsHash()).matches("[0-9a-f]{64}");
    assertThat(event.toString()).doesNotContain(SECRET_VALUE).doesNotContain("Muster");
  }

  @Test
  void an_unavailable_dms_is_a_502_and_is_audited() throws Exception {
    when(validateAttributes.validate(any(), any(), any(), any(), any()))
        .thenThrow(new AdapterUnavailableException("storevalidate answered HTTP 503"));

    check(BODY)
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

    assertThat(audited().eventType()).isEqualTo("validation.check.unavailable");
  }

  @Test
  void without_an_audit_entry_the_caller_gets_no_result() throws Exception {
    when(validateAttributes.validate(any(), any(), any(), any(), any()))
        .thenReturn(ValidationReport.valid(List.of()));
    doThrow(new IllegalStateException("audit down")).when(auditPort).append(any());

    check(BODY)
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.valid").doesNotExist());
  }

  @Test
  void an_unknown_or_foreign_process_is_a_404() throws Exception {
    when(validateAttributes.validate(any(), any(), any(), any(), any()))
        .thenThrow(new ProcessNotFoundException("p1"));

    check(BODY).andExpect(status().isNotFound());

    verify(auditPort, never()).append(any());
  }

  @Test
  void process_and_document_type_are_required() throws Exception {
    check("{\"processId\":\"p1\"}").andExpect(status().isBadRequest());
    check("{\"documentTypeId\":\"DREC\"}").andExpect(status().isBadRequest());

    verify(validateAttributes, never()).validate(any(), any(), any(), any(), any());
  }
}
