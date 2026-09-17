package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import ch.adeon.apps.docextract.agentgateway.application.McpTool;
import ch.adeon.apps.docextract.ingest.application.IngestDocument;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;
import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBack;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import java.util.Map;
import org.springframework.stereotype.Component;

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
    return ingestDocument.ingest(IngestCommand.of(filename, null, content));
  }

  @Override
  public ConfirmationResult confirm(String documentId, boolean consentGiven) {
    return confirmAndWriteBack.confirm(new ConfirmationCommand(documentId, consentGiven, Map.of()));
  }
}
