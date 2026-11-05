package ch.adeon.apps.docextract.agentgateway.application;

import ch.adeon.apps.docextract.ingest.application.IngestedDocument;
import ch.adeon.apps.docextract.validation.application.ConfirmationResult;

public interface McpTool {
  IngestedDocument ingest(String filename, byte[] content);

  ConfirmationResult confirm(
      String processId, String documentId, String repositoryId, String dmsSourceCategory);
}
