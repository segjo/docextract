package ch.adeon.apps.docextract.security.application;

/**
 * The raw header the current request was authenticated with, replayed on outbound DMS calls,
 * together with the requester's {@code Accept-Language} so DMS responses come back translated.
 * {@code acceptLanguage} may be {@code null} if the incoming request did not send one. {@code
 * baseUri} is the d.velop system base URI of the request's tenant (header {@code x-dv-baseuri} or
 * the configured one in single-tenant mode) that all outbound DMS calls go to; there is no fallback
 * to another URI, a credential without one is refused.
 */
public record DvelopCredential(
    String headerName, String headerValue, String acceptLanguage, String baseUri) {

  public DvelopCredential {
    if (baseUri == null || baseUri.isBlank()) {
      throw new IllegalArgumentException("DvelopCredential requires the base URI of the tenant");
    }
  }
}
