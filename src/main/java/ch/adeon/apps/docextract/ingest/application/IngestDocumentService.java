package ch.adeon.apps.docextract.ingest.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.audit.domain.AuditEvent;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;

@Service
public class IngestDocumentService implements IngestDocument {

    private final AuditPort auditPort;

    public IngestDocumentService(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @Override
    public IngestedDocument ingest(IngestCommand command) {
        String documentId = UUID.randomUUID().toString();
        String location = "/dms/documents/" + documentId;
        auditPort.append(new AuditEvent("ingest.completed", "", 0));
        return new IngestedDocument(documentId, location);
    }
}
