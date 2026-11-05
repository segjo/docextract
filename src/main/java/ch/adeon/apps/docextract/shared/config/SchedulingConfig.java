package ch.adeon.apps.docextract.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs (TTL cleanup, promotion retry, ADR-015). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
