package ch.adeon.apps.docextract.validation.application;

import org.springframework.stereotype.Service;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;

@Service
public class ConfirmAndWriteBackService implements ConfirmAndWriteBack {

    private final DmsWritePort dmsWritePort;
    private final AuditPort auditPort;

    public ConfirmAndWriteBackService(DmsWritePort dmsWritePort, AuditPort auditPort) {
        this.dmsWritePort = dmsWritePort;
        this.auditPort = auditPort;
    }

    @Override
    public ConfirmationResult confirm(ConfirmationCommand command) {
        if (!command.consentGiven()) {
            throw new ConsentRequiredException();
        }
        dmsWritePort.writeAttributes(command.documentId(), command.attributes());
        auditPort.append(new AuditEvent("validation.writeback", "", 0));
        return new ConfirmationResult(command.documentId(), true);
    }
}
