package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.adeon.apps.docextract.audit.application.AuditPort;
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
    AuditPort auditPort = mock(AuditPort.class);
    ConfirmAndWriteBackService service =
        new ConfirmAndWriteBackService(dmsWritePort, corpusPromotionPort, auditPort);

    assertThatThrownBy(
            () ->
                service.confirm(
                    new ConfirmationCommand("p1", "doc-1", "repo-1", false, false, Map.of())))
        .isInstanceOf(ConsentRequiredException.class);

    verify(dmsWritePort, never()).writeAttributes("doc-1", Map.of());
  }
}
