package ch.adeon.apps.docextract.security.adapter.in.web;

import static ch.adeon.apps.docextract.security.adapter.in.web.TenantSignatureTest.BASE_URI;
import static ch.adeon.apps.docextract.security.adapter.in.web.TenantSignatureTest.SECRET;
import static ch.adeon.apps.docextract.security.adapter.in.web.TenantSignatureTest.SIGNATURE;
import static ch.adeon.apps.docextract.security.adapter.in.web.TenantSignatureTest.TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.security.adapter.out.session.SessionAuthContextAdapter;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.security.application.IdentityProviderPort;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import ch.adeon.apps.docextract.security.domain.RequestTenant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * NfA-4: single-tenant mode ignores {@code x-dv-*}; multi-tenant mode (app secret set) requires the
 * signed tenant headers and checks the signature before the identityprovider is asked.
 */
class TenantResolutionTest {

  private static final String DEFAULT_TENANT = "default-tenant";
  private static final String SYSTEM_BASE_URI = "http://dms.local";

  private final IdentityProviderPort identityProvider = mock(IdentityProviderPort.class);
  private final AppConfigPort appConfigPort = mock(AppConfigPort.class);
  private final SessionAuthContextAdapter authContext =
      new SessionAuthContextAdapter("agents", "k".repeat(32), "", new MockEnvironment());

  @BeforeEach
  void identityProviderAcceptsTheToken() {
    when(appConfigPort.systemBaseUri()).thenReturn(SYSTEM_BASE_URI);
    DvelopUser user = new DvelopUser("u-1", "user", "User One", List.of());
    when(identityProvider.validateBearerToken(SYSTEM_BASE_URI, "key")).thenReturn(user);
    when(identityProvider.validateBearerToken(BASE_URI, "key")).thenReturn(user);
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void single_tenant_uses_the_default_tenant_and_the_system_base_uri() throws Exception {
    MockHttpServletResponse response = run(singleTenant(), request());

    assertThat(response.getStatus()).isEqualTo(200);
    AuthContext current = authContext.current();
    assertThat(current.tenantId()).isEqualTo(DEFAULT_TENANT);
    assertThat(current.userId()).isEqualTo("u-1");
    assertThat(tenantDetails()).isEqualTo(new RequestTenant(DEFAULT_TENANT, SYSTEM_BASE_URI));
  }

  @Test
  void single_tenant_ignores_forged_dv_headers() throws Exception {
    // A client must not be able to pick its own tenant or base URI without a secret.
    MockHttpServletRequest request = request();
    request.addHeader("x-dv-tenant-id", "forged");
    request.addHeader("x-dv-baseuri", "https://evil.example.com");
    request.addHeader("x-dv-sig-1", "anything");

    run(singleTenant(), request);

    assertThat(authContext.current().tenantId()).isEqualTo(DEFAULT_TENANT);
    assertThat(tenantDetails().baseUri()).isEqualTo(SYSTEM_BASE_URI);
  }

  @Test
  void single_tenant_propagates_the_base_uri_with_the_credential() throws Exception {
    run(singleTenant(), request());

    DvelopCredential credential = (DvelopCredential) credentials();
    assertThat(credential.baseUri()).isEqualTo(SYSTEM_BASE_URI);
  }

  @Test
  void multi_tenant_accepts_the_documented_signature_and_validates_against_its_base_uri()
      throws Exception {
    MockHttpServletResponse response = run(multiTenant(), signedRequest());

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(authContext.current().tenantId()).isEqualTo(TENANT_ID);
    assertThat(tenantDetails()).isEqualTo(new RequestTenant(TENANT_ID, BASE_URI));
    assertThat(((DvelopCredential) credentials()).baseUri()).isEqualTo(BASE_URI);
  }

  @Test
  void multi_tenant_without_any_dv_header_is_forbidden_before_the_identityprovider()
      throws Exception {
    MockHttpServletResponse response = run(multiTenant(), request());

    assertThat(response.getStatus()).isEqualTo(403);
    verifyNoInteractions(identityProvider);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void multi_tenant_with_wrong_signature_is_forbidden_before_the_identityprovider()
      throws Exception {
    MockHttpServletRequest request = signedRequest();
    request.removeHeader("x-dv-sig-1");
    request.addHeader("x-dv-sig-1", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

    assertThat(run(multiTenant(), request).getStatus()).isEqualTo(403);
    verifyNoInteractions(identityProvider);
  }

  @Test
  void multi_tenant_with_a_signature_for_another_tenant_is_forbidden() throws Exception {
    MockHttpServletRequest request = signedRequest();
    request.removeHeader("x-dv-tenant-id");
    request.addHeader("x-dv-tenant-id", "tenant-b");

    assertThat(run(multiTenant(), request).getStatus()).isEqualTo(403);
    verifyNoInteractions(identityProvider);
  }

  @Test
  void multi_tenant_with_a_missing_signature_is_forbidden() throws Exception {
    MockHttpServletRequest request = signedRequest();
    request.removeHeader("x-dv-sig-1");

    assertThat(run(multiTenant(), request).getStatus()).isEqualTo(403);
    verifyNoInteractions(identityProvider);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "a b", "t;DROP", "tenant/../x", "ä"})
  void multi_tenant_with_a_malformed_tenant_id_is_forbidden(String tenantId) throws Exception {
    MockHttpServletRequest request = signedRequest();
    request.removeHeader("x-dv-tenant-id");
    request.addHeader("x-dv-tenant-id", tenantId);

    assertThat(run(multiTenant(), request).getStatus()).isEqualTo(403);
  }

  @Test
  void multi_tenant_never_asks_for_the_configured_system_base_uri() throws Exception {
    run(multiTenant(), signedRequest());

    verifyNoInteractions(appConfigPort);
  }

  @Test
  void multi_tenant_with_a_validly_signed_but_blank_base_uri_is_forbidden_without_fallback()
      throws Exception {
    String signature =
        Base64.getEncoder().encodeToString(TenantSignature.sign(SECRET, " ", TENANT_ID));
    MockHttpServletRequest request = request();
    request.addHeader("x-dv-tenant-id", TENANT_ID);
    request.addHeader("x-dv-baseuri", " ");
    request.addHeader("x-dv-sig-1", signature);

    assertThat(run(multiTenant(), request).getStatus()).isEqualTo(403);
    verifyNoInteractions(appConfigPort, identityProvider);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void credential_without_a_base_uri_is_refused(String baseUri) {
    assertThatThrownBy(() -> new DvelopCredential("Authorization", "Bearer t", null, baseUri))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DvelopCredential("Authorization", "Bearer t", null, null))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(appConfigPort);
  }

  @Test
  void without_authentication_there_is_no_caller_and_no_default_tenant() {
    assertThatThrownBy(authContext::current).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void membership_of_the_agent_group_marks_the_caller_as_agent() throws Exception {
    DvelopUser agent = new DvelopUser("u-2", "agent", "Agent", List.of("agents"));
    when(identityProvider.validateBearerToken(SYSTEM_BASE_URI, "agent-key")).thenReturn(agent);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
    request.addHeader("Authorization", "Bearer agent-key");

    run(singleTenant(), request);

    assertThat(authContext.current().agent()).isTrue();
  }

  @Test
  void a_person_outside_the_agent_group_is_no_agent() throws Exception {
    run(singleTenant(), request());

    assertThat(authContext.current().agent()).isFalse();
  }

  @Test
  void agent_group_none_means_nobody_is_an_agent() throws Exception {
    DvelopUser agent = new DvelopUser("u-2", "agent", "Agent", List.of("none", "agents"));
    when(identityProvider.validateBearerToken(SYSTEM_BASE_URI, "agent-key")).thenReturn(agent);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
    request.addHeader("Authorization", "Bearer agent-key");

    run(singleTenant(), request);

    assertThat(
            new SessionAuthContextAdapter("none", "k".repeat(32), "", new MockEnvironment())
                .current()
                .agent())
        .isFalse();
  }

  private DvelopAuthenticationFilter singleTenant() {
    return new DvelopAuthenticationFilter(identityProvider, appConfigPort, DEFAULT_TENANT, null);
  }

  private DvelopAuthenticationFilter multiTenant() {
    return new DvelopAuthenticationFilter(identityProvider, appConfigPort, DEFAULT_TENANT, SECRET);
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
    request.addHeader("Authorization", "Bearer key");
    return request;
  }

  private static MockHttpServletRequest signedRequest() {
    MockHttpServletRequest request = request();
    request.addHeader("x-dv-tenant-id", TENANT_ID);
    request.addHeader("x-dv-baseuri", BASE_URI);
    request.addHeader("x-dv-sig-1", SIGNATURE);
    return request;
  }

  private static MockHttpServletResponse run(
      DvelopAuthenticationFilter filter, MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  private static RequestTenant tenantDetails() {
    return (RequestTenant) authentication().getDetails();
  }

  private static Object credentials() {
    return authentication().getCredentials();
  }

  private static Authentication authentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}
