package ch.adeon.apps.docextract.security.domain;

/**
 * The raw header the current request was authenticated with, replayed on outbound DMS calls,
 * together with the requester's {@code Accept-Language} so DMS responses come back translated.
 * {@code acceptLanguage} may be {@code null} if the incoming request did not send one.
 */
public record DvelopCredential(String headerName, String headerValue, String acceptLanguage) {}
