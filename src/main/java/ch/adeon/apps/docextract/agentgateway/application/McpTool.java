package ch.adeon.apps.docextract.agentgateway.application;

import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;

public interface McpTool {
    IngestedDocument ingest(String filename, byte[] content);

    ConfirmationResult confirm(String documentId, boolean consentGiven);
}
