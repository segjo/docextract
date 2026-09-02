package com.adeon.docextract.validation.application;

import org.springframework.stereotype.Service;

import com.adeon.docextract.audit.application.AuditPort;
import com.adeon.docextract.audit.domain.AuditEvent;
import com.adeon.docextract.validation.domain.ConfirmationCommand;
import com.adeon.docextract.validation.domain.ConfirmationResult;

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
