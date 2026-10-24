package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.port.CorpusPromotionPort;
import ch.adeon.apps.docextract.validation.port.DmsWritePort;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfirmAndWriteBackServiceTest {

  @Test
  void must_not_write_without_consent() {
    DmsWritePort dmsWritePort = mock(DmsWritePort.class);
    CorpusPromotionPort corpusPromotionPort = mock(CorpusPromotionPort.class);
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    AuditPort auditPort = mock(AuditPort.class);
    ConfirmAndWriteBackService service =
        new ConfirmAndWriteBackService(
            dmsWritePort,
            corpusPromotionPort,
            outboundCredentialPort,
            auditPort,
            "https://dms.local",
            "DBUE",
            "document.pdf");

    assertThatThrownBy(
            () ->
                service.confirm(
                    new ConfirmationCommand("p1", "doc-1", "repo-1", false, false, Map.of())))
        .isInstanceOf(ConsentRequiredException.class);

    verify(dmsWritePort, never())
        .writeAttributes(eq("repo-1"), eq(""), eq(Map.of()), any(), any(), any(), any());
  }

  @Test
  void must_reject_and_not_promote_when_dms_rejects_attributes() {
    DmsWritePort dmsWritePort = mock(DmsWritePort.class);
    CorpusPromotionPort corpusPromotionPort = mock(CorpusPromotionPort.class);
    OutboundCredentialPort outboundCredentialPort = mock(OutboundCredentialPort.class);
    AuditPort auditPort = mock(AuditPort.class);
    when(outboundCredentialPort.current())
        .thenReturn(new DvelopCredential("Authorization", "Bearer token", null));
    when(dmsWritePort.writeAttributes(
            eq("repo-1"), eq(""), eq(Map.of()), any(), any(), any(), any()))
        .thenReturn(null);
    ConfirmAndWriteBackService service =
        new ConfirmAndWriteBackService(
            dmsWritePort,
            corpusPromotionPort,
            outboundCredentialPort,
            auditPort,
            "https://dms.local",
            "DBUE",
            "document.pdf");

    assertThatThrownBy(
            () ->
                service.confirm(
                    new ConfirmationCommand("p1", "doc-1", "repo-1", true, false, Map.of())))
        .isInstanceOf(InvalidAttributesException.class);

    verify(corpusPromotionPort, never()).promote("p1", "repo-1", null);
  }
}
