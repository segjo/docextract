package ch.adeon.apps.docextract.shared.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.adeon.apps.docextract.extraction.adapter.in.rest.ExtractionController;
import ch.adeon.apps.docextract.extraction.application.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.ExtractionResultPort;
import ch.adeon.apps.docextract.extraction.application.GetExtractionResultService;
import ch.adeon.apps.docextract.extraction.application.RetryExtraction;
import ch.adeon.apps.docextract.process.adapter.in.sse.ProcessEventsController;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.adapter.in.rest.RetrievalController;
import ch.adeon.apps.docextract.retrieval.application.DocumentTypeCandidatesPort;
import ch.adeon.apps.docextract.retrieval.application.GetDocumentTypeCandidatesService;
import ch.adeon.apps.docextract.retrieval.application.GetObjectDefinitions;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocumentsService;
import ch.adeon.apps.docextract.retrieval.application.GetValueList;
import ch.adeon.apps.docextract.retrieval.application.RetrievalResultPort;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.shared.adapter.web.GlobalExceptionHandler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
/**
 * NfA-4: every process-scoped REST endpoint hands the caller's tenant to the port. The ports hold
 * data per tenant, so a caller of another tenant gets what an unknown process gets: nothing.
 */
class TenantScopedEndpointsTest {

  private static final String PROCESS = "p1";

  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final RetrievalResultPort retrievalResultPort = mock(RetrievalResultPort.class);
  private final DocumentTypeCandidatesPort candidatesPort = mock(DocumentTypeCandidatesPort.class);
  private final ExtractionResultPort extractionResultPort = mock(ExtractionResultPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final RetryExtraction retryExtraction = mock(RetryExtraction.class);

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    when(outboundCredentialPort.current())
        .thenReturn(new DvelopCredential("Authorization", "Bearer t", null, "https://dms.local"));
    // data exists for tenant-a only; tenant-b's own process id "p-b" exists without results
    when(processEventPort.exists("tenant-a", PROCESS)).thenReturn(true);
    when(processEventPort.exists("tenant-b", "p-b")).thenReturn(true);
    when(retrievalResultPort.find("tenant-a", PROCESS))
        .thenReturn(Optional.of(List.of(new RetrievalResult("repo-1", "doc-1", 0.9, null))));
    when(extractionResultPort.find("tenant-a", PROCESS))
        .thenReturn(
            Optional.of(
                new ExtractionResult(
                    "DREC", List.of(new ExtractedAttribute("p1", "v", null, 0.8, "x")))));
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ProcessEventsController(processEventPort, authContextPort),
                new RetrievalController(
                    new GetSimilarDocumentsService(retrievalResultPort, processEventPort),
                    new GetDocumentTypeCandidatesService(candidatesPort, processEventPort),
                    mock(GetObjectDefinitions.class),
                    mock(GetValueList.class),
                    authContextPort),
                new ExtractionController(
                    new GetExtractionResultService(extractionResultPort, processEventPort),
                    retryExtraction,
                    outboundCredentialPort,
                    authContextPort))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void owner_reads_the_similar_documents() throws Exception {
    callerIs("tenant-a");

    mockMvc
        .perform(get("/api/v1/processes/{id}/similar-documents", PROCESS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].documentId").value("doc-1"));
  }

  @Test
  void another_tenant_gets_404_for_similar_documents() throws Exception {
    callerIs("tenant-b");

    mockMvc
        .perform(get("/api/v1/processes/{id}/similar-documents", PROCESS))
        .andExpect(status().isNotFound());
    verify(retrievalResultPort, never()).find(eq("tenant-a"), any());
  }

  @Test
  void own_process_without_results_yet_gets_an_empty_list() throws Exception {
    callerIs("tenant-b");

    mockMvc
        .perform(get("/api/v1/processes/{id}/similar-documents", "p-b"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void another_tenant_gets_404_for_document_type_candidates() throws Exception {
    callerIs("tenant-b");

    mockMvc
        .perform(get("/api/v1/processes/{id}/document-type-candidates", PROCESS))
        .andExpect(status().isNotFound());
    verify(candidatesPort).find("tenant-b", PROCESS);
    verify(candidatesPort, never()).find(eq("tenant-a"), any());
  }

  @Test
  void owner_reads_the_extraction_result() throws Exception {
    callerIs("tenant-a");

    mockMvc
        .perform(get("/api/v1/processes/{id}/extraction-result", PROCESS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentTypeId").value("DREC"));
  }

  @Test
  void another_tenant_gets_404_for_the_extraction_result() throws Exception {
    callerIs("tenant-b");

    mockMvc
        .perform(get("/api/v1/processes/{id}/extraction-result", PROCESS))
        .andExpect(status().isNotFound());
    verify(extractionResultPort, never()).find(eq("tenant-a"), any());
  }

  @Test
  void own_process_without_extraction_result_yet_gets_an_empty_result() throws Exception {
    callerIs("tenant-b");

    mockMvc
        .perform(get("/api/v1/processes/{id}/extraction-result", "p-b"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentTypeId").doesNotExist())
        .andExpect(jsonPath("$.attributes").isEmpty());
  }

  @Test
  void extraction_retry_runs_for_the_callers_tenant_only() throws Exception {
    callerIs("tenant-b");
    when(retryExtraction.retry(eq("tenant-b"), eq(PROCESS), eq("DREC"), any()))
        .thenThrow(new ProcessNotFoundException(PROCESS));

    mockMvc
        .perform(
            post("/api/v1/processes/{id}/extraction-result/retry", PROCESS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentTypeId\":\"DREC\"}"))
        .andExpect(status().isNotFound());

    verify(retryExtraction, never()).retry(eq("tenant-a"), any(), any(), any());
  }

  @Test
  void process_events_of_another_tenant_are_404_and_never_subscribed() throws Exception {
    callerIs("tenant-b");

    mockMvc
        .perform(get("/api/v1/processes/{id}/events", PROCESS).accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isNotFound());

    verify(processEventPort, never()).subscribe(any(), any(), any());
  }

  @Test
  void process_events_are_subscribed_for_the_callers_tenant_only() throws Exception {
    callerIs("tenant-a");

    mockMvc
        .perform(get("/api/v1/processes/{id}/events", PROCESS).accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isOk());

    verify(processEventPort).subscribe(eq("tenant-a"), eq(PROCESS), any());
    verify(processEventPort, never()).subscribe(eq("tenant-b"), any(), any());
  }

  private void callerIs(String tenantId) {
    when(authContextPort.current()).thenReturn(new AuthContext(tenantId, "user-1", "User"));
  }
}
