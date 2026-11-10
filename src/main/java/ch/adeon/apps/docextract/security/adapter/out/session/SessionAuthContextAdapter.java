package ch.adeon.apps.docextract.security.adapter.out.session;

import ch.adeon.apps.docextract.security.application.AuthContext;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AgentGroup;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import ch.adeon.apps.docextract.security.domain.RequestTenant;
import ch.adeon.apps.docextract.shared.hash.Hmac;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Request-bound caller from the security context; only valid on the request thread. The async
 * pipeline gets the tenant passed explicitly. Without an authenticated request there is no caller:
 * an {@link IllegalStateException}, never a default tenant.
 *
 * <p>{@code actorHash} = HMAC-SHA256 over tenant and user id with {@code
 * docextract.audit.actor-key} (at least {@value #MIN_KEY_LENGTH} characters, different from the app
 * secret). The key is mandatory; only the {@code dev} profile falls back to a development key.
 */
@Component
public class SessionAuthContextAdapter implements AuthContextPort {

  private static final Logger log = LoggerFactory.getLogger(SessionAuthContextAdapter.class);
  static final String DEV_ACTOR_KEY = "docextract-dev-actor-key-not-for-production";
  static final int MIN_KEY_LENGTH = 32;

  private final String agentAuthority;
  private final byte[] actorKey;

  public SessionAuthContextAdapter(
      @Value("${docextract.agent.group-id:}") String agentGroupId,
      @Value("${docextract.audit.actor-key:}") String actorKey,
      @Value("${docextract.tenant.app-secret:}") String appSecret,
      Environment environment) {
    this.agentAuthority = AgentGroup.authority(agentGroupId);
    boolean keyMissing = actorKey == null || actorKey.isBlank();
    if (keyMissing && !environment.acceptsProfiles(Profiles.of("dev"))) {
      throw new IllegalStateException(
          "docextract.audit.actor-key must be set (at least " + MIN_KEY_LENGTH + " characters)");
    }
    boolean multiTenant = appSecret != null && !appSecret.isBlank();
    if (keyMissing && multiTenant) {
      throw new IllegalStateException(
          "docextract.audit.actor-key must be set when docextract.tenant.app-secret is set");
    }
    if (!keyMissing && actorKey.length() < MIN_KEY_LENGTH) {
      throw new IllegalStateException(
          "docextract.audit.actor-key must have at least " + MIN_KEY_LENGTH + " characters");
    }
    if (!keyMissing && multiTenant && actorKey.equals(appSecret)) {
      throw new IllegalStateException(
          "docextract.audit.actor-key must differ from docextract.tenant.app-secret");
    }
    if (keyMissing) {
      log.warn(
          "docextract.audit.actor-key is not set: using the development key (profile dev), audit"
              + " actor hashes are recomputable by anyone");
    }
    this.actorKey = (keyMissing ? DEV_ACTOR_KEY : actorKey).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public AuthContext current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof DvelopUser dvelopUser)
        || !(authentication.getDetails() instanceof RequestTenant tenant)) {
      throw new IllegalStateException("No authenticated request context (wrong thread?)");
    }
    boolean agent =
        agentAuthority != null
            && authentication.getAuthorities().stream()
                .anyMatch(granted -> agentAuthority.equals(granted.getAuthority()));
    return new AuthContext(
        tenant.id(),
        dvelopUser.id(),
        dvelopUser.displayName(),
        agent,
        Hmac.sha256Hex(actorKey, tenant.id() + "\n" + dvelopUser.id()));
  }
}
