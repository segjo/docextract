package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.adeon.apps.docextract.ingest.application.IngestDocument;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final IngestDocument ingestDocument;

    public IngestController(IngestDocument ingestDocument) {
        this.ingestDocument = ingestDocument;
    }

    @PostMapping
    public ResponseEntity<IngestedDocument> ingest(@RequestBody IngestRequest request) {
        IngestedDocument ingested = ingestDocument.ingest(new IngestCommand(request.filename(), request.content()));
        return ResponseEntity.accepted().body(ingested);
    }

    public record IngestRequest(String filename, byte[] content) {
    }
}
