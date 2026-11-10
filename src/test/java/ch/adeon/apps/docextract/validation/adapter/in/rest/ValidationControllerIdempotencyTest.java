package ch.adeon.apps.docextract.validation.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.shared.web.GlobalExceptionHandler;
import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBackService;
import ch.adeon.apps.docextract.validation.application.FinalizationStatePort;
import ch.adeon.apps.docextract.validation.application.RejectSuggestion;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import ch.adeon.apps.docextract.validation.port.PendingEmbeddingCheckPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * x-idempotency of POST /api/v1/validation/confirm (doc/api/openapi.yaml, ADR-015): HTTP layer plus
 * the real saga service; only the outbound ports are replaced, the saga state is kept in memory.
 */
class ValidationControllerIdempotencyTest {

  private static final String CONFIRM_BODY =
      """
      {"processId":"p1","dmsSourceCategory":"DREC","attributes":{"1":"x"}}
      """;

  private DmsWritePort dmsWritePort;
  private CorpusPromotionPort corpusPromotionPort;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    dmsWritePort = mock(DmsWritePort.class);
    corpusPromotionPort = mock(CorpusPromotionPort.class);
    AuthContextPort authContextPort = mock(AuthContextPort.class);
    when(authContextPort.current()).thenReturn(new AuthContext("tenant-a", "user-1", "User"));
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    when(outboundCredentialPort.current())
        .thenReturn(
            new DvelopCredential("Authorization", "Bearer token", null, "https://dms.local"));
    ProcessDocumentPort processDocumentPort = mock(ProcessDocumentPort.class);
    when(processDocumentPort.find("tenant-a", "p1"))
        .thenReturn(Optional.of(new StoredDocument("/blob/1", "a.pdf")));
    PendingEmbeddingCheckPort pendingEmbeddingCheckPort = mock(PendingEmbeddingCheckPort.class);
    when(pendingEmbeddingCheckPort.find("tenant-a", "p1"))
        .thenReturn(Optional.of(new PendingEmbeddingCheck(false)));
    ProcessRepositoryPort processRepositoryPort = mock(ProcessRepositoryPort.class);
    when(processRepositoryPort.retrieve("tenant-a", "p1")).thenReturn(Optional.of("repo-1"));
    when(dmsWritePort.writeAttributes(any(), any(), any(), any(), any(), any()))
        .thenReturn("doc-1");

    ConfirmAndWriteBackService service =
        new ConfirmAndWriteBackService(
            dmsWritePort,
            corpusPromotionPort,
            outboundCredentialPort,
            authContextPort,
            mock(AuditPort.class),
            processRepositoryPort,
            processDocumentPort,
            new InMemoryFinalizationState(),
            pendingEmbeddingCheckPort,
            mock(DocumentBlobPort.class),
            "default-repo",
            Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC),
            Duration.ofHours(24),
            Duration.ofHours(1));
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ValidationController(service, mock(RejectSuggestion.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void repeated_confirm_writes_to_the_dms_exactly_once_and_returns_the_same_result()
      throws Exception {
    String first = confirm();
    String second = confirm();

    assertThat(second).isEqualTo(first);
    verify(dmsWritePort, times(1)).writeAttributes(any(), any(), any(), any(), any(), any());
    verify(corpusPromotionPort, times(1)).promote("tenant-a", "p1", "repo-1", "doc-1");
  }

  @Test
  void repeated_confirm_in_finalized_index_pending_only_retries_the_promotion() throws Exception {
    doThrow(new IllegalStateException("db down"))
        .doNothing()
        .when(corpusPromotionPort)
        .promote(anyString(), anyString(), anyString(), anyString());

    mockMvc
        .perform(confirmRequest())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indexStatus").value("FINALIZED_INDEX_PENDING"));
    mockMvc
        .perform(confirmRequest())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId").value("doc-1"))
        .andExpect(jsonPath("$.writtenBack").value(true))
        .andExpect(jsonPath("$.indexStatus").value("COMPLETED"));

    verify(dmsWritePort, times(1)).writeAttributes(any(), any(), any(), any(), any(), any());
    verify(corpusPromotionPort, times(2)).promote("tenant-a", "p1", "repo-1", "doc-1");
  }

  private static MockHttpServletRequestBuilder confirmRequest() {
    return post("/api/v1/validation/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .content(CONFIRM_BODY);
  }

  private String confirm() throws Exception {
    return mockMvc
        .perform(confirmRequest())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.repositoryId").value("repo-1"))
        .andExpect(jsonPath("$.documentId").value("doc-1"))
        .andExpect(jsonPath("$.indexStatus").value("COMPLETED"))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  /** Stateful stand-in for process_finalization, so the second call sees the first's state. */
  private static final class InMemoryFinalizationState implements FinalizationStatePort {

    private final Map<String, Finalization> rows = new ConcurrentHashMap<>();

    @Override
    public void save(Finalization finalization) {
      rows.put(finalization.processId(), finalization);
    }

    @Override
    public Optional<Finalization> find(String tenantId, String processId) {
      return Optional.ofNullable(rows.get(processId)).filter(f -> f.tenantId().equals(tenantId));
    }

    @Override
    public List<Finalization> findByStatus(FinalizationStatus status) {
      return rows.values().stream().filter(f -> f.status() == status).toList();
    }

    @Override
    public void discardWriteStarted(String tenantId, String processId) {
      rows.computeIfPresent(
          processId,
          (id, f) ->
              f.tenantId().equals(tenantId) && f.status() == FinalizationStatus.DMS_WRITE_STARTED
                  ? null
                  : f);
    }

    @Override
    public void deleteByTenant(String tenantId) {
      rows.values().removeIf(f -> f.tenantId().equals(tenantId));
    }
  }
}
