package ch.adeon.apps.docextract.security.application;

import ch.adeon.apps.docextract.security.domain.DvelopCredential;

/** Exposes the current request's d.velop credential so outbound adapters can replay it. */
public interface OutboundCredentialPort {

  DvelopCredential current();
}
