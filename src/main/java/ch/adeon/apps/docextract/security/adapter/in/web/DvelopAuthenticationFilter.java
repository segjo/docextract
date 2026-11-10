package ch.adeon.apps.docextract.security.adapter.in.web;

import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.AgentGroup;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import ch.adeon.apps.docextract.security.domain.RequestTenant;
import ch.adeon.apps.docextract.shared.error.ProblemCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
/**
 * Resolves the tenant of every request, then validates it against the identityprovider (Bearer
 * header or AuthSessionId cookie) of that tenant (NfA-4).
 *
 * <p>Single-tenant (no app secret): the configured default tenant and system base URI apply, {@code
 * x-dv-*} headers are ignored. Multi-tenant (app secret set): {@code x-dv-tenant-id}, {@code
 * x-dv-baseuri} and {@code x-dv-sig-1} are mandatory and the signature is checked <em>before</em>
 * anything else, otherwise 403.
 */
public class DvelopAuthenticationFilter extends OncePerRequestFilter {

  static final String TENANT_ID_HEADER = "x-dv-tenant-id";
  static final String BASE_URI_HEADER = "x-dv-baseuri";
  static final String SIGNATURE_HEADER = "x-dv-sig-1";

  private static final String AUTH_SESSION_COOKIE = "AuthSessionId";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
  private static final String FORBIDDEN_BODY =
      "{\"type\":\""
          + ProblemCode.FORBIDDEN.typeUri()
          + "\",\"title\":\"Forbidden\",\"status\":403,"
          + "\"detail\":\"Invalid or missing tenant headers\"}";

  private final IdentityProviderPort identityProviderPort;
  private final AppConfigPort appConfigPort;
  private final String defaultTenantId;
  private final byte[] appSecret;

  /** {@code appSecret} is the base64-decoded app secret, {@code null} for single-tenant. */
  public DvelopAuthenticationFilter(
      IdentityProviderPort identityProviderPort,
      AppConfigPort appConfigPort,
      String defaultTenantId,
      byte[] appSecret) {
    this.identityProviderPort = identityProviderPort;
    this.appConfigPort = appConfigPort;
    this.defaultTenantId = defaultTenantId;
    this.appSecret = appSecret;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    RequestTenant signedTenant = null;
    if (appSecret != null) {
      signedTenant = verifiedTenant(request);
      if (signedTenant == null) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.getOutputStream().write(FORBIDDEN_BODY.getBytes(StandardCharsets.UTF_8));
        return;
      }
    }
    try {
      Authenticated authenticated = authenticate(request, signedTenant);
      if (authenticated != null) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                authenticated.user(),
                authenticated.credential(),
                toAuthorities(authenticated.user()));
        authentication.setDetails(authenticated.tenant());
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (AuthenticationException ex) {
      SecurityContextHolder.clearContext();
    }
    filterChain.doFilter(request, response);
  }

  /** The tenant of a request carrying valid signed headers, otherwise {@code null}. */
  private RequestTenant verifiedTenant(HttpServletRequest request) {
    String tenantId = request.getHeader(TENANT_ID_HEADER);
    String baseUri = request.getHeader(BASE_URI_HEADER);
    String signature = request.getHeader(SIGNATURE_HEADER);
    if (tenantId == null
        || baseUri == null
        || baseUri.isBlank()
        || signature == null
        || !TENANT_ID.matcher(tenantId).matches()
        || !TenantSignature.matches(appSecret, baseUri, tenantId, signature)) {
      return null;
    }
    return new RequestTenant(tenantId, baseUri);
  }

  private static List<GrantedAuthority> toAuthorities(DvelopUser dvelopUser) {
    return dvelopUser.groupIds().stream()
        .map(groupId ->
            (GrantedAuthority) new SimpleGrantedAuthority(AgentGroup.AUTHORITY_PREFIX + groupId))
        .toList();
  }

  private Authenticated authenticate(HttpServletRequest request, RequestTenant signedTenant) {
    String acceptLanguage = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    String authorizationHeader = request.getHeader("Authorization");
    if (authorizationHeader != null
        && authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      RequestTenant tenant = tenantOf(signedTenant);
      DvelopUser dvelopUser =
          identityProviderPort.validateBearerToken(
              tenant.baseUri(), authorizationHeader.substring(BEARER_PREFIX.length()));
      return new Authenticated(
          dvelopUser,
          new DvelopCredential(
              HttpHeaders.AUTHORIZATION, authorizationHeader, acceptLanguage, tenant.baseUri()),
          tenant);
    }
    String authSessionId = extractAuthSessionCookie(request);
    if (authSessionId != null) {
      RequestTenant tenant = tenantOf(signedTenant);
      DvelopUser dvelopUser =
          identityProviderPort.validateSessionCookie(tenant.baseUri(), authSessionId);
      return new Authenticated(
          dvelopUser,
          new DvelopCredential(
              HttpHeaders.COOKIE,
              AUTH_SESSION_COOKIE + "=" + authSessionId,
              acceptLanguage,
              tenant.baseUri()),
          tenant);
    }
    return null;
  }

  /** Single-tenant mode resolves the base URI only when it is needed. */
  private RequestTenant tenantOf(RequestTenant signedTenant) {
    return signedTenant != null
        ? signedTenant
        : new RequestTenant(defaultTenantId, appConfigPort.systemBaseUri());
  }

  /**
   * The identity resolved for this request, together with the raw credential to replay downstream
   * and the tenant it belongs to.
   */
  private record Authenticated(
      DvelopUser user, DvelopCredential credential, RequestTenant tenant) {}

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
