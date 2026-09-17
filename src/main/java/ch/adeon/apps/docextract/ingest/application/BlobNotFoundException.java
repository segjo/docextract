package ch.adeon.apps.docextract.ingest.application;

import java.util.UUID;

/** Thrown when no blob (or no longer, e.g. after TTL cleanup) exists for a given id. */
public class BlobNotFoundException extends RuntimeException {

  public BlobNotFoundException(UUID blobId) {
    super("No blob found for id " + blobId);
  }
}
