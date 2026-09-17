package ch.adeon.apps.docextract.security.adapter.out.session;

import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SessionCredentialAdapter implements OutboundCredentialPort {

  @Override
  public DvelopCredential current() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getCredentials() instanceof DvelopCredential credential) {
      return credential;
    }
    throw new IllegalStateException("No d.velop credential available on the current request");
  }
}
