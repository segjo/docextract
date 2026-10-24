package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.ImportInfo;

/**
 * Inbound port: everything the import mask needs to render before the user picks a file (accepted
 * file extensions, max upload size, selectable repositories) — called once when the mask opens.
 */
public interface GetImportInfo {

  ImportInfo get();
}
