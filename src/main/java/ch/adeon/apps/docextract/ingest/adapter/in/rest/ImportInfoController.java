package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import ch.adeon.apps.docextract.ingest.application.GetImportInfo;
import ch.adeon.apps.docextract.ingest.domain.ImportInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /ingest/import-info} — called once when the import mask opens, so the frontend never
 * hardcodes the accepted file extensions, the max upload size, or the repository list (FR-1):
 * accepted file extensions and the max upload size mirror what {@code POST /documents} actually
 * enforces, and the repositories are the current user's own d.velop RepoApp connections ({@code GET
 * /repo/repositories}), live for the current session.
 */
@RestController
@RequestMapping("/api/v1/ingest")
public class ImportInfoController {

  private final GetImportInfo getImportInfo;

  public ImportInfoController(GetImportInfo getImportInfo) {
    this.getImportInfo = getImportInfo;
  }

  @GetMapping("/import-info")
  public ImportInfo importInfo() {
    return getImportInfo.get();
  }
}
