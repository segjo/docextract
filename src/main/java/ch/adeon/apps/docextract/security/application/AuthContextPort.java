package ch.adeon.apps.docextract.security.application;

import ch.adeon.apps.docextract.security.domain.AuthContext;

public interface AuthContextPort {
    AuthContext current();
}
