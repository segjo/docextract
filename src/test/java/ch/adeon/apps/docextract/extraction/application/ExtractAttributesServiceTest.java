package ch.adeon.apps.docextract.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.content.application.ContentStagingPort;
import ch.adeon.apps.docextract.content.domain.Representation;
import ch.adeon.apps.docextract.content.port.DocumentContent;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import ch.adeon.apps.docextract.extraction.domain.ModelInfo;
import ch.adeon.apps.docextract.extraction.port.LlmPort;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.retrieval.application.DocumentTypeCandidatesPort;
import ch.adeon.apps.docextract.retrieval.application.RetrievalResultPort;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ExtractAttributesServiceTest {

  private static final DvelopCredential CREDENTIAL =
      new DvelopCredential("Authorization", "Bearer t", null, "https://dms.local");
  private static final ModelInfo MODEL = new ModelInfo("openai-compatible", "qwen3");

  private final DocumentTypeCandidatesPort candidatesPort = mock(DocumentTypeCandidatesPort.class);
  private final ContentStagingPort contentStagingPort = mock(ContentStagingPort.class);
  private final ExtractionResultPort extractionResultPort = mock(ExtractionResultPort.class);
  private final LlmPort llmPort = mock(LlmPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private final ExtractAttributesService service =
      new ExtractAttributesService(
          candidatesPort,
          contentStagingPort,
          mock(ValueListPort.class),
          extractionResultPort,
          mock(RetrievalResultPort.class),
          llmPort,
          processEventPort,
          auditPort,
          JsonMapper.builder().build(),
          mock(ProcessRepositoryPort.class),
          "repo-1",
          50,
          10);

  private static DmsPropertyDefinition writable(String id) {
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

  private static DmsDocumentMetadata candidate(String typeId) {
    return new DmsDocumentMetadata(
        new DmsDocumentType(typeId, typeId, List.of(writable("p1"))), null, List.of());
  }

  @Test
  void llm_events_carry_the_tenant_of_the_pipeline_and_the_model() {
    when(candidatesPort.find("tenant-a", "proc-1"))
        .thenReturn(Optional.of(List.of(candidate("DREC"), candidate("DLET"))));
    when(contentStagingPort.retrieve("proc-1"))
        .thenReturn(
            Optional.of(new DocumentContent(Representation.FULLTEXT, "Rechnung Muster AG")));
    when(llmPort.generate(anyString(), anyString(), anyMap()))
        .thenReturn(new LlmResponse("{\"documentTypeId\":\"DREC\"}", MODEL, 11));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(
            new LlmResponse(
                "{\"attributes\":[{\"propertyId\":\"p1\",\"value\":null,\"values\":null,"
                    + "\"confidence\":0.5,\"sourceExcerpt\":null}]}",
                MODEL,
                22));

    service.extract("tenant-a", "proc-1", CREDENTIAL);

    ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort, org.mockito.Mockito.times(2)).append(events.capture());
    assertThat(events.getAllValues())
        .extracting(AuditEvent::eventType)
        .containsExactly("extraction.classify", "extraction.suggest-attributes");
    assertThat(events.getAllValues())
        .allSatisfy(event -> {
          assertThat(event.tenantId()).isEqualTo("tenant-a");
          assertThat(event.channel()).isEqualTo(AuditChannel.SYSTEM);
          assertThat(event.provider()).isEqualTo("openai-compatible");
          assertThat(event.modelId()).isEqualTo("qwen3");
          assertThat(event.argsHash()).hasSize(64).doesNotContain("Muster");
        });
    assertThat(events.getAllValues()).extracting(AuditEvent::tokenCount).containsExactly(11, 22);
  }

  @Test
  void retry_of_an_unknown_or_foreign_process_is_not_found_and_does_nothing() {
    when(candidatesPort.find("tenant-b", "proc-1")).thenReturn(Optional.empty());
    when(processEventPort.exists("tenant-b", "proc-1")).thenReturn(false);

    assertThatThrownBy(() -> service.retry("tenant-b", "proc-1", "DREC", CREDENTIAL))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(processEventPort, never()).publish(any(), any());
    verify(llmPort, never()).generate(any(), any(), any());
    verify(extractionResultPort, never()).store(any(), any(), any());
  }

  @Test
  void retry_of_an_own_process_without_candidates_is_an_extraction_error_not_a_404() {
    when(candidatesPort.find("tenant-a", "proc-1")).thenReturn(Optional.empty());
    when(processEventPort.exists("tenant-a", "proc-1")).thenReturn(true);

    assertThatThrownBy(() -> service.retry("tenant-a", "proc-1", "DREC", CREDENTIAL))
        .isNotInstanceOf(ProcessNotFoundException.class);
  }
}
