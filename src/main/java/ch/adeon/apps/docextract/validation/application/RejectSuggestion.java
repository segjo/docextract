package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.validation.domain.RejectionCommand;

/** Inbound port: rejects a process's suggestion (FR-5) — never a DMS write, only cleanup. */
public interface RejectSuggestion {

  void reject(RejectionCommand command);
}
