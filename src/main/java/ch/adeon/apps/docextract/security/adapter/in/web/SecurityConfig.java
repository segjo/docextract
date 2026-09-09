package ch.adeon.apps.docextract.security.adapter.in.web;

import java.io.IOException;

import tools.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ch.adeon.apps.docextract.security.application.IdentityProviderPort;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Requires every /api/** request to carry a valid d.velop session, validated
 * per-request via {@link IdentityProviderPort}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, IdentityProviderPort identityProviderPort,
                        ObjectMapper objectMapper) throws Exception {
                http
                                .securityMatcher("/api/**")
                                // no local session cookie is issued; every request is independently
                                // re-validated
                                // against the identityprovider, so CSRF tokens tied to server-side session
                                // state don't apply
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                                .exceptionHandling(handling -> handling
                                                .authenticationEntryPoint((request, response,
                                                                authException) -> writeProblemDetail(
                                                                                response, objectMapper,
                                                                                HttpStatus.UNAUTHORIZED,
                                                                                "Session validation failed"))
                                                .accessDeniedHandler((request, response,
                                                                accessDeniedException) -> writeProblemDetail(
                                                                                response, objectMapper,
                                                                                HttpStatus.FORBIDDEN, "Access denied")))
                                .addFilterBefore(new DvelopAuthenticationFilter(identityProviderPort),
                                                UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        private void writeProblemDetail(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
                        String detail) throws IOException {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
                response.setStatus(status.value());
                response.setContentType("application/problem+json");
                objectMapper.writeValue(response.getWriter(), problem);
        }
}
