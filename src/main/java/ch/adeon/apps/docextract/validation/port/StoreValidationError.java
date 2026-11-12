package ch.adeon.apps.docextract.validation.port;

/**
 * The one error the DMS reports per check. {@code dmsCode} is the code in square brackets of the
 * message, passed through unmapped (the codes are configuration-dependent). {@code message} is DMS
 * text for the caller only: never logged or audited (C-4). All fields may be {@code null}.
 */
public record StoreValidationError(String propertyId, String dmsCode, String message) {}
