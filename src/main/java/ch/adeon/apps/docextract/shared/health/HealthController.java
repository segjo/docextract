package ch.adeon.apps.docextract.shared.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
  @Tag(name = "operations")
  @SecurityRequirements
  @Operation(
      operationId = "health",
      summary = "Betriebsstatus der Abhängigkeiten (ohne Authentisierung)")
  public SystemHealth health() {
    return systemHealthService.check();
  }
}
