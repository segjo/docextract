package ch.adeon.apps.docextract.agentgateway.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.ActorKind;
import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.application.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.shared.error.InvalidRequestException;
import ch.adeon.apps.docextract.shared.error.ProblemCode;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
class AgentGatewayServiceTest {

  private static final String PROCESS_ID = "3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b";
  private static final String SECRET_VALUE = "Muster AG, Bahnhofstrasse 1";
  private static final String ACTOR_HASH = "a".repeat(64);

  private final GetExtractionResult getExtractionResult = mock(GetExtractionResult.class);
  private final GetSimilarDocuments getSimilarDocuments = mock(GetSimilarDocuments.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);

  private final AgentGatewayService service =
      new AgentGatewayService(getExtractionResult, getSimilarDocuments, authContextPort, auditPort);

  @BeforeEach
  void callerIsAgentOfTenantA() {
    when(authContextPort.current())
        .thenReturn(new AuthContext("tenant-a", "agent", "Agent", true, ACTOR_HASH));
  }

  @Test
  void returns_suggestions_of_own_process_via_same_read_service_as_rest() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID))
        .thenReturn(
            new ExtractionResult(
                "DREC",
                List.of(new ExtractedAttribute("p1", SECRET_VALUE, null, 0.82, "excerpt"))));

    ProcessSuggestions suggestions = service.getProcessSuggestions(PROCESS_ID);

    assertThat(suggestions.documentTypeId()).isEqualTo("DREC");
    assertThat(suggestions.attributes())
        .containsExactly(
            new ProcessSuggestions.AttributeSuggestion("p1", SECRET_VALUE, null, 0.82));
    assertThat(auditedEvent().eventType()).isEqualTo("mcp.tool.get_process_suggestions.ok");
  }

  @Test
  void returns_similar_documents_without_dms_metadata() {
    when(getSimilarDocuments.get("tenant-a", PROCESS_ID))
        .thenReturn(List.of(new RetrievalResult("repo-1", "doc-1", 0.93, "{\"name\":\"x\"}")));

    assertThat(service.findSimilarDocuments(PROCESS_ID))
        .containsExactly(new SimilarDocument("doc-1", "repo-1", 0.93));
    assertThat(auditedEvent().eventType()).isEqualTo("mcp.tool.find_similar_documents.ok");
  }

  // The read services answer unknown and foreign processes alike with ProcessNotFoundException.
  @Test
  void unknown_or_foreign_process_is_not_found() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID))
        .thenThrow(new ProcessNotFoundException(PROCESS_ID));
    when(getSimilarDocuments.get("tenant-a", PROCESS_ID))
        .thenThrow(new ProcessNotFoundException(PROCESS_ID));

    assertThatThrownBy(() -> service.getProcessSuggestions(PROCESS_ID))
        .isInstanceOf(ProcessNotFoundException.class);
    assertThatThrownBy(() -> service.findSimilarDocuments(PROCESS_ID))
        .isInstanceOf(ProcessNotFoundException.class);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {"", "not-a-uuid", "3F2B8C1E-4A5D-4E6F-8A9B-0C1D2E3F4A5B", PROCESS_ID + " "})
  void invalid_process_id_is_rejected_before_any_lookup(String processId) {
    assertThatThrownBy(() -> service.getProcessSuggestions(processId))
        .isInstanceOfSatisfying(
            InvalidRequestException.class,
            ex -> assertThat(ex.code()).isEqualTo(ProblemCode.INVALID_REQUEST));

    verifyNoInteractions(getExtractionResult, getSimilarDocuments);
    assertThat(auditedEvent().eventType())
        .isEqualTo("mcp.tool.get_process_suggestions.invalid_argument");
  }

  @Test
  void audit_entry_of_an_agent_call_names_channel_tenant_and_actor_kind() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID)).thenReturn(ExtractionResult.EMPTY);

    service.getProcessSuggestions(PROCESS_ID);

    AuditEvent event = auditedEvent();
    assertThat(event.channel()).isEqualTo(AuditChannel.MCP);
    assertThat(event.actorKind()).isEqualTo(ActorKind.AGENT);
    assertThat(event.tenantId()).isEqualTo("tenant-a");
    assertThat(event.actorHash()).isEqualTo(ACTOR_HASH);
  }

  @Test
  void audit_entry_of_a_person_on_the_mcp_endpoint_says_human() {
    when(authContextPort.current())
        .thenReturn(new AuthContext("tenant-a", "user-1", "User One", false));
    when(getExtractionResult.get("tenant-a", PROCESS_ID)).thenReturn(ExtractionResult.EMPTY);

    service.getProcessSuggestions(PROCESS_ID);

    AuditEvent event = auditedEvent();
    assertThat(event.channel()).isEqualTo(AuditChannel.MCP);
    assertThat(event.actorKind()).isEqualTo(ActorKind.HUMAN);
  }

  @Test
  void audit_entry_carries_only_hashes_no_attribute_values_or_user_id() {
    when(getExtractionResult.get("tenant-a", PROCESS_ID))
        .thenReturn(
            new ExtractionResult(
                "DREC", List.of(new ExtractedAttribute("p1", SECRET_VALUE, null, 0.9, "x"))));

    service.getProcessSuggestions(PROCESS_ID);

    AuditEvent event = auditedEvent();
    assertThat(event.argsHash()).isEqualTo(Sha256.hex(PROCESS_ID.getBytes(StandardCharsets.UTF_8)));
    assertThat(event.toString())
        .doesNotContain(SECRET_VALUE)
        .doesNotContain(PROCESS_ID)
        .doesNotContain("agent,");
  }

  private AuditEvent auditedEvent() {
    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort).append(captor.capture());
    return captor.getValue();
  }
}
