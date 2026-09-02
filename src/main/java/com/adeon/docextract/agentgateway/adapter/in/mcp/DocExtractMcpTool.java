package com.adeon.docextract.agentgateway.adapter.in.mcp;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.adeon.docextract.agentgateway.application.McpTool;
import com.adeon.docextract.ingest.application.IngestDocument;
import com.adeon.docextract.ingest.domain.IngestCommand;
import com.adeon.docextract.ingest.domain.IngestedDocument;
import com.adeon.docextract.validation.application.ConfirmAndWriteBack;
import com.adeon.docextract.validation.domain.ConfirmationCommand;
import com.adeon.docextract.validation.domain.ConfirmationResult;

@Component
public class DocExtractMcpTool implements McpTool {

    private final IngestDocument ingestDocument;
    private final ConfirmAndWriteBack confirmAndWriteBack;

    public DocExtractMcpTool(IngestDocument ingestDocument, ConfirmAndWriteBack confirmAndWriteBack) {
        this.ingestDocument = ingestDocument;
        this.confirmAndWriteBack = confirmAndWriteBack;
    }

    @Override
    public IngestedDocument ingest(String filename, byte[] content) {
        return ingestDocument.ingest(new IngestCommand(filename, content));
    }

    @Override
    public ConfirmationResult confirm(String documentId, boolean consentGiven) {
        return confirmAndWriteBack.confirm(new ConfirmationCommand(documentId, consentGiven, Map.of()));
    }
}
