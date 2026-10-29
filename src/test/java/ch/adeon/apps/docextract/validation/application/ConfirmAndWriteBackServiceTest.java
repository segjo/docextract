package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConfirmAndWriteBackServiceTest {

  @Test
  void must_reject_and_not_promote_when_dms_rejects_attributes() {
    DmsWritePort dmsWritePort = mock(DmsWritePort.class);
    CorpusPromotionPort corpusPromotionPort = mock(CorpusPromotionPort.class);
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    AuditPort auditPort = mock(AuditPort.class);
    AppConfigPort appConfigPort = mock(AppConfigPort.class);
    ProcessDocumentPort processDocumentPort = mock(ProcessDocumentPort.class);
    ProcessRepositoryPort processRepositoryPort = mock(ProcessRepositoryPort.class);
    when(processDocumentPort.find("p1"))
        .thenReturn(Optional.of(new StoredDocument("/blob/1", "a.pdf")));
    when(processRepositoryPort.retrieve("p1")).thenReturn(Optional.of("repo-1"));
    when(outboundCredentialPort.current())
        .thenReturn(new DvelopCredential("Authorization", "Bearer token", null));
    when(dmsWritePort.writeAttributes(
            eq("repo-1"), eq("/blob/1"), eq(Map.of()), eq("a.pdf"), eq("DREC"), any()))
        .thenReturn(null);
    ConfirmAndWriteBackService service =
        new ConfirmAndWriteBackService(
            dmsWritePort,
            corpusPromotionPort,
            outboundCredentialPort,
            appConfigPort,
            auditPort,
            processRepositoryPort,
            processDocumentPort,
            "default-repo");

    assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("p1", "DREC", Map.of())))
        .isInstanceOf(InvalidAttributesException.class);

    verify(corpusPromotionPort, never()).promote(eq("p1"), any(), any());
  }
}
