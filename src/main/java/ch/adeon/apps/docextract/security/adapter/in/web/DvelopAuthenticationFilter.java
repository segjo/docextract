package ch.adeon.apps.docextract.security.adapter.in.web;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.DvelopUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Validates every request against the identityprovider (Bearer header or
 * AuthSessionId cookie).
 */
public class DvelopAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_SESSION_COOKIE = "AuthSessionId";
    private static final String BEARER_PREFIX = "Bearer ";

    private final IdentityProviderPort identityProviderPort;

    public DvelopAuthenticationFilter(IdentityProviderPort identityProviderPort) {
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            DvelopUser dvelopUser = authenticate(request);
            if (dvelopUser != null) {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        dvelopUser, null, toAuthorities(dvelopUser));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private static List<GrantedAuthority> toAuthorities(DvelopUser dvelopUser) {
        return dvelopUser.groupIds().stream()
                .map(groupId -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_GROUP_" + groupId))
                .toList();
    }

    private DvelopUser authenticate(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null
                && authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return identityProviderPort.validateBearerToken(authorizationHeader.substring(BEARER_PREFIX.length()));
        }
        String authSessionId = extractAuthSessionCookie(request);
        if (authSessionId != null) {
            return identityProviderPort.validateSessionCookie(authSessionId);
        }
        return null;
    }

    private String extractAuthSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_SESSION_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
