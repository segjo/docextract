package ch.adeon.apps.docextract.security.application;


/** Exposes the current request's d.velop credential so outbound adapters can replay it. */
public interface OutboundCredentialPort {

  DvelopCredential current();
}
