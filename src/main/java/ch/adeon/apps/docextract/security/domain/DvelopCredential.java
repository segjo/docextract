package ch.adeon.apps.docextract.security.domain;

/** The raw header the current request was authenticated with, replayed on outbound DMS calls. */
public record DvelopCredential(String headerName, String headerValue) {}
