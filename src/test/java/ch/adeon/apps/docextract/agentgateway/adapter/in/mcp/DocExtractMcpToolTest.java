package ch.adeon.apps.docextract.agentgateway.adapter.in.mcp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import ch.adeon.apps.docextract.ingest.application.IngestDocument;
import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBack;

class DocExtractMcpToolTest {

    @Test
    void uses_same_application_services_as_rest_path() {
        IngestDocument ingestDocument = mock(IngestDocument.class);
        ConfirmAndWriteBack confirmAndWriteBack = mock(ConfirmAndWriteBack.class);
        DocExtractMcpTool tool = new DocExtractMcpTool(ingestDocument, confirmAndWriteBack);

        tool.ingest("test.pdf", new byte[] { 1, 2 });
        tool.confirm("doc-1", true);

        verify(ingestDocument).ingest(org.mockito.ArgumentMatchers.any());
        verify(confirmAndWriteBack).confirm(org.mockito.ArgumentMatchers.any());
    }
}
