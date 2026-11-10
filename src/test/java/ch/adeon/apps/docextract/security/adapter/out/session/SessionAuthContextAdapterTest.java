package ch.adeon.apps.docextract.security.adapter.out.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import ch.adeon.apps.docextract.security.domain.RequestTenant;
import ch.adeon.apps.docextract.shared.hash.Hmac;
import ch.adeon.apps.docextract.shared.hash.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** C-4: the audit actor is a keyed pseudonym; the key is mandatory outside profile dev. */
class SessionAuthContextAdapterTest {

  private static final String APP_SECRET = "ptuQ0b0BskmLLxXsjjhH9Su8ozTvZl6Z/5/HlaORoRg=";
  private static final String KEY_1 = "k".repeat(32);
  private static final String KEY_2 = "j".repeat(32);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static MockEnvironment prod() {
    return new MockEnvironment();
  }

  private static MockEnvironment dev() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("dev");
    return environment;
  }

  private static SessionAuthContextAdapter adapter(String key, String appSecret) {
    return new SessionAuthContextAdapter("none", key, appSecret, prod());
  }

  @Test
  void actor_hash_is_hmac_sha256_over_tenant_and_user_with_the_actor_key() {
    AuthContext context = callerOf(adapter(KEY_1, ""));

    assertThat(context.actorHash())
        .isEqualTo(Hmac.sha256Hex(KEY_1.getBytes(StandardCharsets.UTF_8), "tenant-a\nuser-1"))
        .hasSize(64);
  }

  @Test
  void actor_hash_is_not_the_plain_sha256_and_depends_on_the_key() {
    String plain = Sha256.hex("tenant-a\nuser-1".getBytes(StandardCharsets.UTF_8));

    String hash1 = callerOf(adapter(KEY_1, "")).actorHash();
    String hash2 = callerOf(adapter(KEY_2, "")).actorHash();

    assertThat(hash1).isNotEqualTo(plain).isNotEqualTo(hash2);
  }

  @Test
  void actor_hash_differs_per_tenant_and_user() {
    SessionAuthContextAdapter adapter = adapter(KEY_1, "");

    String a = callerOf(adapter, "tenant-a", "user-1").actorHash();

    assertThat(callerOf(adapter, "tenant-b", "user-1").actorHash()).isNotEqualTo(a);
    assertThat(callerOf(adapter, "tenant-a", "user-2").actorHash()).isNotEqualTo(a);
    assertThat(callerOf(adapter, "tenant-a", "user-1").actorHash()).isEqualTo(a);
  }

  @Test
  void profile_dev_without_key_uses_the_development_key() {
    SessionAuthContextAdapter adapter = new SessionAuthContextAdapter("none", "", "", dev());

    assertThat(callerOf(adapter).actorHash())
        .isEqualTo(
            Hmac.sha256Hex(
                SessionAuthContextAdapter.DEV_ACTOR_KEY.getBytes(StandardCharsets.UTF_8),
                "tenant-a\nuser-1"));
  }

  @Test
  void without_profile_dev_a_missing_key_aborts_the_startup() {
    assertThatThrownBy(() -> adapter("", ""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("docextract.audit.actor-key");
    assertThatThrownBy(() -> adapter("  ", "")).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> adapter(null, "")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void a_key_shorter_than_32_characters_aborts_the_startup_in_every_profile() {
    String shortKey = "k".repeat(31);

    assertThatThrownBy(() -> adapter(shortKey, ""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32");
    assertThatThrownBy(() -> new SessionAuthContextAdapter("none", shortKey, "", dev()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void a_key_of_exactly_32_characters_is_accepted() {
    assertThat(callerOf(adapter(KEY_1, "")).actorHash()).hasSize(64);
  }

  @Test
  void with_app_secret_a_missing_key_aborts_the_startup_even_in_profile_dev() {
    assertThatThrownBy(() -> new SessionAuthContextAdapter("none", "", APP_SECRET, dev()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("app-secret");
  }

  @Test
  void the_app_secret_must_not_be_reused_as_actor_key() {
    assertThatThrownBy(() -> adapter(APP_SECRET, APP_SECRET))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("differ");
  }

  @Test
  void with_app_secret_and_a_separate_key_it_starts() {
    assertThat(callerOf(adapter(KEY_1, APP_SECRET)).actorHash()).isNotBlank();
  }

  private static AuthContext callerOf(SessionAuthContextAdapter adapter) {
    return callerOf(adapter, "tenant-a", "user-1");
  }

  private static AuthContext callerOf(
      SessionAuthContextAdapter adapter, String tenantId, String userId) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            new DvelopUser(userId, "user", "User", List.of()), "credential", List.of());
    authentication.setDetails(new RequestTenant(tenantId, "http://dms.local"));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    return adapter.current();
  }
}
