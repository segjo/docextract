package ch.adeon.apps.docextract.security.adapter.in.web;

import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.AgentGroup;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Requires every /api/** request to carry a valid d.velop session, validated per-request via {@link
 * IdentityProviderPort}. The MCP endpoint is open to every authenticated user (person and agent);
 * all other /api/** are closed to members of the agent group (§ 8.6, C-2, T-6).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  @Bean
  public SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http,
      IdentityProviderPort identityProviderPort,
      AppConfigPort appConfigPort,
      ObjectMapper objectMapper,
      @Value("${docextract.tenant.default-id}") String defaultTenantId,
      @Value("${docextract.tenant.app-secret:}") String appSecret,
      @Value("${docextract.agent.group-id:}") String agentGroupId,
      @Value("${docextract.adapters.mcp.enabled:false}") boolean mcpEnabled,
      @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/api/v1/mcp}") String mcpEndpoint)
      throws Exception {
    byte[] secret = decodeAppSecret(appSecret);
    if (secret == null) {
      log.info(
          "Single-tenant mode: tenant '{}' is fixed, x-dv-* tenant headers are ignored,"
              + " system base URI from the configuration (docextract.tenant.app-secret is empty)",
          defaultTenantId);
    }
    String agentAuthority = AgentGroup.authority(agentGroupId);
    if (mcpEnabled && (agentGroupId == null || agentGroupId.isBlank())) {
      throw new IllegalStateException(
          "docextract.agent.group-id must be set when MCP is enabled; use '"
              + AgentGroup.NONE
              + "' for deliberately no agent group");
    }
    http.securityMatcher("/api/**")
        // no local session cookie is issued; every request is independently
        // re-validated
        // against the identityprovider, so CSRF tokens tied to server-side session
        // state don't apply
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> {
          // re-dispatch of an already authorized streaming response (SSE, MCP);
          // the stateless context is gone there
          authorize.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll();
          if (agentAuthority == null) {
            authorize.anyRequest().authenticated();
          } else {
            authorize
                .requestMatchers(mcpEndpoint, mcpEndpoint + "/**")
                .authenticated()
                .anyRequest()
                .access(notForAgents(agentAuthority));
          }
        })
        .exceptionHandling(handling ->
            handling
                .authenticationEntryPoint((request, response, authException) ->
                    writeProblemDetail(
                        response,
                        objectMapper,
                        HttpStatus.UNAUTHORIZED,
                        "Session validation failed"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeProblemDetail(
                        response, objectMapper, HttpStatus.FORBIDDEN, "Access denied")))
        .addFilterBefore(
            new DvelopAuthenticationFilter(
                identityProviderPort, appConfigPort, defaultTenantId, secret),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /** Authenticated and not a member of the agent group. */
  private static AuthorizationManager<RequestAuthorizationContext> notForAgents(
      String agentAuthority) {
    AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
    return (authentication, context) -> {
      Authentication caller = authentication.get();
      boolean authenticated =
          caller != null && caller.isAuthenticated() && !trustResolver.isAnonymous(caller);
      boolean agent =
          authenticated
              && caller.getAuthorities().stream()
                  .anyMatch(granted -> agentAuthority.equals(granted.getAuthority()));
      return new AuthorizationDecision(authenticated && !agent);
    };
  }

  /** {@code null} for a blank secret (single-tenant); the secret itself is never logged. */
  private static byte[] decodeAppSecret(String appSecret) {
    if (appSecret == null || appSecret.isBlank()) {
      return null;
    }
    try {
      return Base64.getDecoder().decode(appSecret.trim());
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("docextract.tenant.app-secret is not valid base64");
    }
  }

  private void writeProblemDetail(
      HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status, String detail)
      throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    objectMapper.writeValue(response.getWriter(), problem);
  }
}
