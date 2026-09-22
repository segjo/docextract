package ch.adeon.apps.docextract.shared.health;

/**
 * One dependency's reachability result; {@code detail} is only populated when {@code status} is
 * DOWN.
 */
public record ComponentHealth(String name, HealthStatus status, String detail) {

  static ComponentHealth up(String name) {
    return new ComponentHealth(name, HealthStatus.UP, null);
  }

  static ComponentHealth down(String name, String detail) {
    return new ComponentHealth(name, HealthStatus.DOWN, detail);
  }
}
