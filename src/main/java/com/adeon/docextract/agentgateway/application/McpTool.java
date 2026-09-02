package com.adeon.docextract.agentgateway.application;

import com.adeon.docextract.ingest.domain.IngestedDocument;
import com.adeon.docextract.validation.domain.ConfirmationResult;

public interface McpTool {
    IngestedDocument ingest(String filename, byte[] content);

    ConfirmationResult confirm(String documentId, boolean consentGiven);
}
