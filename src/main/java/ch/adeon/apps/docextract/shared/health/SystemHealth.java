package ch.adeon.apps.docextract.shared.health;

import java.util.List;

public record SystemHealth(HealthStatus status, List<ComponentHealth> components) {

  static SystemHealth of(List<ComponentHealth> components) {
    boolean allUp = components.stream().allMatch(c -> c.status() == HealthStatus.UP);
    return new SystemHealth(allUp ? HealthStatus.UP : HealthStatus.DOWN, components);
  }
}
