package ch.adeon.apps.docextract.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Injectable time source, so time-based rules (e.g. retry limits) are testable. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
