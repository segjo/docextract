package ch.adeon.apps.docextract.security.domain;

/**
 * Tenant of the current request with the d.velop system base URI to reach its apps; set as
 * authentication details by the filter, in single-tenant mode from the configuration.
 */
public record RequestTenant(String id, String baseUri) {}
