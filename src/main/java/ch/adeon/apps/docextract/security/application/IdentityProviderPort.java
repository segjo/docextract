package ch.adeon.apps.docextract.security.application;

import ch.adeon.apps.docextract.security.domain.DvelopUser;

/**
 * Outbound port to validate a d.velop AuthSessionId/Bearer token against the
 * identityprovider app.
 */
public interface IdentityProviderPort {

    DvelopUser validateBearerToken(String bearerToken);

    DvelopUser validateSessionCookie(String authSessionId);
}
