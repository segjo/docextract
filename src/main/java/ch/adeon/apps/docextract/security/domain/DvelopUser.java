package ch.adeon.apps.docextract.security.domain;

import java.util.List;

/**
 * User information as returned by the d.velop identityprovider /validate
 * endpoint.
 */
public record DvelopUser(String id, String userName, String displayName, List<String> groupIds) {
}
