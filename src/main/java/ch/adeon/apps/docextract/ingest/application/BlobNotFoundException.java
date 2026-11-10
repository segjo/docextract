package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.shared.error.NotFoundException;
import java.util.UUID;
/** Thrown when no blob (or no longer, e.g. after TTL cleanup) exists for a given id. */
public class BlobNotFoundException extends NotFoundException {

  public BlobNotFoundException(UUID blobId) {
    super("No blob found for id " + blobId, null);
  }
}
