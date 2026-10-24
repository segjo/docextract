package ch.adeon.apps.docextract.ingest.domain;

import java.util.List;

/**
 * Everything the import mask needs to render before the user picks a file (FR-1): the file
 * extensions ingest can actually accept ({@link
 * ch.adeon.apps.docextract.ingest.application.GeneratePreview#supportedFileExtensions()}), the hard
 * upload size cap ({@link Limits}), and the repositories the current user may import into ({@link
 * DmsRepository}).
 */
public record ImportInfo(
    List<String> acceptedFileExtensions, long maxFileSizeBytes, List<DmsRepository> repositories) {}
