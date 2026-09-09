package ch.adeon.apps.docextract.security.adapter.out.session;

import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import ch.adeon.apps.docextract.security.domain.DvelopUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthContextAdapter implements AuthContextPort {

  @Override
  public AuthContext current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof DvelopUser dvelopUser) {
      String aclRef = String.join(",", dvelopUser.groupIds());
      return new AuthContext("default-tenant", aclRef, dvelopUser.id(), dvelopUser.displayName());
    }
    return new AuthContext("default-tenant", "default-acl", "system", "system");
  }
}
