package ch.adeon.apps.docextract.agentgateway.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.agentgateway.application.AgentToolException.Code;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.extraction.application.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class AgentGatewayServiceTest {

  private static final String PROCESS_ID = "3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b";
  private static final String SECRET_VALUE = "Muster AG, Bahnhofstrasse 1";

  private final GetExtractionResult getExtractionResult = mock(GetExtractionResult.class);
  private final GetSimilarDocuments getSimilarDocuments = mock(GetSimilarDocuments.class);
  private final ProcessDocumentPort processDocumentPort = mock(ProcessDocumentPort.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);

  private final AgentGatewayService service =
      new AgentGatewayService(
          getExtractionResult,
          getSimilarDocuments,
          processDocumentPort,
          authContextPort,
          auditPort);

  @BeforeEach
  void callerIsTenantA() {
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "agent", "Agent"));
  }

  @Test
  void returns_suggestions_of_own_process_via_same_read_service_as_rest() {
    ownedBy("tenant-a");
    when(getExtractionResult.get(PROCESS_ID))
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
    ownedBy("tenant-a");
    when(getSimilarDocuments.get(PROCESS_ID))
        .thenReturn(List.of(new RetrievalResult("repo-1", "doc-1", 0.93, "{\"name\":\"x\"}")));

    assertThat(service.findSimilarDocuments(PROCESS_ID))
        .containsExactly(new SimilarDocument("doc-1", "repo-1", 0.93));
    assertThat(auditedEvent().eventType()).isEqualTo("mcp.tool.find_similar_documents.ok");
  }

  @Test
  void foreign_tenant_process_is_not_found_and_no_data_is_read() {
    ownedBy("tenant-b");

    assertThatThrownBy(() -> service.getProcessSuggestions(PROCESS_ID))
        .isInstanceOfSatisfying(
            AgentToolException.class, ex -> assertThat(ex.code()).isEqualTo(Code.NOT_FOUND));
    assertThatThrownBy(() -> service.findSimilarDocuments(PROCESS_ID))
        .isInstanceOf(AgentToolException.class);

    verifyNoInteractions(getExtractionResult, getSimilarDocuments);
  }

  @Test
  void unknown_process_is_not_found() {
    when(processDocumentPort.find(PROCESS_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findSimilarDocuments(PROCESS_ID))
        .isInstanceOfSatisfying(
            AgentToolException.class, ex -> assertThat(ex.code()).isEqualTo(Code.NOT_FOUND));
    assertThat(auditedEvent().eventType()).isEqualTo("mcp.tool.find_similar_documents.not_found");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {"", "not-a-uuid", "3F2B8C1E-4A5D-4E6F-8A9B-0C1D2E3F4A5B", PROCESS_ID + " "})
  void invalid_process_id_is_rejected_before_any_lookup(String processId) {
    assertThatThrownBy(() -> service.getProcessSuggestions(processId))
        .isInstanceOfSatisfying(
            AgentToolException.class, ex -> assertThat(ex.code()).isEqualTo(Code.INVALID_ARGUMENT));

    verify(processDocumentPort, never()).find(anyString());
    verifyNoInteractions(getExtractionResult);
    assertThat(auditedEvent().eventType())
        .isEqualTo("mcp.tool.get_process_suggestions.invalid_argument");
  }

  @Test
  void audit_entry_carries_only_the_argument_hash() {
    ownedBy("tenant-a");
    when(getExtractionResult.get(PROCESS_ID))
        .thenReturn(
            new ExtractionResult(
                "DREC", List.of(new ExtractedAttribute("p1", SECRET_VALUE, null, 0.9, "x"))));

    service.getProcessSuggestions(PROCESS_ID);

    AuditEvent event = auditedEvent();
    assertThat(event.promptHash())
        .isEqualTo(Sha256.hex(PROCESS_ID.getBytes(StandardCharsets.UTF_8)));
    assertThat(event.toString()).doesNotContain(SECRET_VALUE).doesNotContain(PROCESS_ID);
  }

  private void ownedBy(String tenantId) {
    when(processDocumentPort.find(PROCESS_ID))
        .thenReturn(Optional.of(new StoredDocument("/dms/r/repo/blob/chunk", "a.pdf", tenantId)));
  }

  private AuditEvent auditedEvent() {
    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort).append(captor.capture());
    return captor.getValue();
  }
}
