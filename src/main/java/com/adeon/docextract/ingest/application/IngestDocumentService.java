package com.adeon.docextract.ingest.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.adeon.docextract.audit.application.AuditPort;
import com.adeon.docextract.audit.domain.AuditEvent;
import com.adeon.docextract.ingest.domain.IngestCommand;
import com.adeon.docextract.ingest.domain.IngestedDocument;

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
