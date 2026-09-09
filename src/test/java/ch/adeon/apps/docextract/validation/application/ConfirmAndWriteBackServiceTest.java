package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;

class ConfirmAndWriteBackServiceTest {

    @Test
    void must_not_write_without_consent() {
        DmsWritePort dmsWritePort = mock(DmsWritePort.class);
        AuditPort auditPort = mock(AuditPort.class);
        ConfirmAndWriteBackService service = new ConfirmAndWriteBackService(dmsWritePort, auditPort);

        assertThatThrownBy(() -> service.confirm(new ConfirmationCommand("doc-1", false, Map.of())))
                .isInstanceOf(ConsentRequiredException.class);

        verify(dmsWritePort, never()).writeAttributes("doc-1", Map.of());
    }
}
