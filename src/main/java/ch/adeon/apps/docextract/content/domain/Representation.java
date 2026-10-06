package ch.adeon.apps.docextract.content.domain;

/** Document representation shape for FR-4, matched to the configured LLM adapter's capabilities. */
public enum Representation {
  FULLTEXT,
  MARKDOWN,
  PAGE_IMAGES
}
