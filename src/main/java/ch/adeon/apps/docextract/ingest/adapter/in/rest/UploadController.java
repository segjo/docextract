package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import ch.adeon.apps.docextract.ingest.application.IngestDocument;
import ch.adeon.apps.docextract.ingest.domain.IngestCommand;
import ch.adeon.apps.docextract.ingest.domain.IngestedDocument;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@code POST /documents} — synchronous blob-store write + original-chunk-upload, then {@code 202
 * Accepted + processId} (FR-1, ADR-008). The actual DMS object is only finalized later, after human
 * consent (validation module).
 */
@RestController
@RequestMapping("/api/v1/ingest/documents")
public class UploadController {

  private final IngestDocument ingestDocument;
  private final AuthContextPort authContextPort;

  public UploadController(IngestDocument ingestDocument, AuthContextPort authContextPort) {
    this.ingestDocument = ingestDocument;
    this.authContextPort = authContextPort;
  }

  @GetMapping("/test")
  public ResponseEntity<String> getMethodName(@RequestParam(required = false) String param) {
    AuthContext auth = authContextPort.current();
    return ResponseEntity.ok().body("hello " + auth.displayName());
  }

  @PostMapping(consumes = "multipart/form-data")
  public ResponseEntity<IngestedDocument> upload(@RequestParam("file") MultipartFile file)
      throws IOException {
    IngestCommand command =
        IngestCommand.of(file.getOriginalFilename(), file.getContentType(), file.getBytes());
    IngestedDocument ingested = ingestDocument.ingest(command);
    return ResponseEntity.accepted().body(ingested);
  }
}
