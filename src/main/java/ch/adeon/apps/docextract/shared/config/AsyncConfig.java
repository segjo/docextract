package ch.adeon.apps.docextract.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async} for post-upload work (e.g. preview generation) off the request thread. */
@Configuration
@EnableAsync
public class AsyncConfig {}
