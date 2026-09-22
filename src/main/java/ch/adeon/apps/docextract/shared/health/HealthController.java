package ch.adeon.apps.docextract.shared.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated operator-facing status endpoint (outside {@code /api/**}, so it is not covered by
 * {@code SecurityConfig}'s d.velop session check) — mirrors the static {@code index.html} test page
 * it is rendered on.
 */
@RestController
public class HealthController {

  private final SystemHealthService systemHealthService;

  public HealthController(SystemHealthService systemHealthService) {
    this.systemHealthService = systemHealthService;
  }

  @GetMapping("/health")
  public SystemHealth health() {
    return systemHealthService.check();
  }
}
