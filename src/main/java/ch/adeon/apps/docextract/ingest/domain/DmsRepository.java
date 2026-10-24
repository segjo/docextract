package ch.adeon.apps.docextract.ingest.domain;

/**
 * A d.velop repository connection the current user can import into, as returned by the RepoApp's
 * {@code GET /repo/repositories} (doc/api/dvelop-repoapp.md). Reduced to the fields the import
 * mask's repository selector needs — {@code isAvailable} tells whether the current user exists in
 * that repository, {@code isReachable} whether the repository is currently reachable at all.
 */
public record DmsRepository(
    String id, String name, boolean isDefault, boolean isAvailable, boolean isReachable) {}
