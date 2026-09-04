package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.adeon.apps.docextract.ingest.application.IngestDocument;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/ingest")
public class IngestController {

    private final IngestDocument ingestDocument;
    private final AuthContextPort authContextPort;

    public IngestController(IngestDocument ingestDocument, AuthContextPort authContextPort) {
        this.ingestDocument = ingestDocument;
        this.authContextPort = authContextPort;
    }

    @GetMapping("/test")
    public ResponseEntity<String> getMethodName(@RequestParam(required = false) String param) {
        AuthContext auth = authContextPort.current();
        return ResponseEntity.ok().body("hello " + auth.displayName());
    }

    @PostMapping
    public ResponseEntity<IngestedDocument> ingest(@RequestBody IngestRequest request) {
        IngestedDocument ingested = ingestDocument.ingest(new IngestCommand(request.filename(), request.content()));
        return ResponseEntity.accepted().body(ingested);
    }

    public record IngestRequest(String filename, byte[] content) {
    }
}
