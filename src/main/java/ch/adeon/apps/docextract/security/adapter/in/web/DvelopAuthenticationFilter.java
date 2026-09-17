package ch.adeon.apps.docextract.security.adapter.in.web;

import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates every request against the identityprovider (Bearer header or AuthSessionId cookie). */
public class DvelopAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTH_SESSION_COOKIE = "AuthSessionId";
  private static final String BEARER_PREFIX = "Bearer ";

  private final IdentityProviderPort identityProviderPort;

  public DvelopAuthenticationFilter(IdentityProviderPort identityProviderPort) {
    this.identityProviderPort = identityProviderPort;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      Authenticated authenticated = authenticate(request);
      if (authenticated != null) {
        Authentication authentication =
            new UsernamePasswordAuthenticationToken(
                authenticated.user(),
                authenticated.credential(),
                toAuthorities(authenticated.user()));
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

  private Authenticated authenticate(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader != null
        && authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      DvelopUser dvelopUser =
          identityProviderPort.validateBearerToken(
              authorizationHeader.substring(BEARER_PREFIX.length()));
      return new Authenticated(
          dvelopUser, new DvelopCredential(HttpHeaders.AUTHORIZATION, authorizationHeader));
    }
    String authSessionId = extractAuthSessionCookie(request);
    if (authSessionId != null) {
      DvelopUser dvelopUser = identityProviderPort.validateSessionCookie(authSessionId);
      return new Authenticated(
          dvelopUser,
          new DvelopCredential(HttpHeaders.COOKIE, AUTH_SESSION_COOKIE + "=" + authSessionId));
    }
    return null;
  }

  /**
   * The identity resolved for this request, together with the raw credential to replay downstream.
   */
  private record Authenticated(DvelopUser user, DvelopCredential credential) {}

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
