package ch.adeon.apps.docextract.security.application;

import org.springframework.stereotype.Component;

import ch.adeon.apps.docextract.security.domain.AuthContext;

@Component
public class SessionAuthContextAdapter implements AuthContextPort {

    @Override
    public AuthContext current() {
        return new AuthContext("default-tenant", "default-acl", "system");
    }
}
