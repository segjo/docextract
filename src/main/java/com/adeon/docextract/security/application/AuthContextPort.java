package com.adeon.docextract.security.application;

import com.adeon.docextract.security.domain.AuthContext;

public interface AuthContextPort {
    AuthContext current();
}
