package ch.adeon.apps.docextract.shared.config;

import ch.adeon.apps.docextract.shared.error.ProblemCode;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Drives springdoc-openapi (/v3/api-docs, /swagger-ui.html), generated from the actual
 * {@code @RestController}s. The snapshot doc/api/openapi.yaml is checked against it by {@code
 * OpenApiContractTest} (drift check).
 */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "DocExtract API",
            version = "v1",
            description =
                "Eigener REST-Vertrag von DocExtract. Fehler werden als application/problem+json"
                    + " (RFC 9457, Schema `Problem`, `type` = urn:docextract:problem:<code>)"
                    + " beschrieben; siehe ARCHITECTURE.md § 8.1."),
    servers = @Server(url = "/adeon-docextract", description = "Context-Path hinter dem Proxy"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

  public static final String PROBLEM_JSON = "application/problem+json";
  public static final String PROBLEM_SCHEMA_REF = "#/components/schemas/Problem";
  public static final String BAD_REQUEST_REF = "#/components/responses/BadRequest";
  public static final String UNAUTHORIZED_REF = "#/components/responses/Unauthorized";
  public static final String BAD_GATEWAY_REF = "#/components/responses/BadGateway";

  /** Shared RFC 9457 schema and the responses every /api endpoint can return. */
  @Bean
  OpenApiCustomizer problemDetailCustomizer() {
    return openApi -> {
      if (openApi.getTags() != null) {
        openApi.getTags().sort(Comparator.comparing(Tag::getName));
      }
      Components components = openApi.getComponents();
      components.addSchemas("Problem", problemSchema());
      components.addResponses(
          "BadRequest",
          problemResponse(
              "Ungültige Anfrage: fehlender/ungültiger Parameter oder nicht lesbarer Body"
                  + " (type `urn:docextract:problem:invalid-request`)."));
      components.addResponses(
          "BadGateway",
          problemResponse(
              "Ein angebundenes System (z. B. d.velop DMS) ist nicht erreichbar oder antwortet"
                  + " fehlerhaft (type `urn:docextract:problem:adapter-unavailable`, detail ohne"
                  + " Upstream-Inhalt)."));
      components.addResponses(
          "Unauthorized",
          problemResponse(
              "Keine gültige d.velop-Session (Bearer-Token oder Cookie AuthSessionId);"
                  + " geliefert von SecurityConfig."));
    };
  }

  private static ApiResponse problemResponse(String description) {
    return new ApiResponse()
        .description(description)
        .content(
            new Content()
                .addMediaType(
                    PROBLEM_JSON,
                    new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref(PROBLEM_SCHEMA_REF))));
  }

  // Schema#setProperties takes the raw Map<String, Schema>.
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Schema<Object> problemSchema() {
    Map<String, Schema> properties = new LinkedHashMap<>();
    properties.put(
        "type",
        typed("string")
            .format("uri")
            .description(
                "URI-Referenz auf den Problemtyp (RFC 9457 § 3.1.1): urn:docextract:problem:<code>"
                    + " mit code = "
                    + Arrays.stream(ProblemCode.values())
                        .map(ProblemCode::slug)
                        .collect(Collectors.joining(", "))
                    + "."));
    properties.put("title", typed("string").description("Kurzbeschreibung des Problemtyps."));
    properties.put("status", typed("integer").format("int32").description("HTTP-Statuscode."));
    properties.put(
        "detail",
        typed("string").description("Erläuterung dieses Vorkommens, ohne Roh-PII (C-4)."));
    properties.put("instance", typed("string").format("uri").description("URI dieses Vorkommens."));
    properties.put(
        "processId",
        typed("string").description("Erweiterung: betroffener Prozess, falls vorhanden."));
    Schema<Object> problem = typed("object");
    problem.setProperties(properties);
    problem.setRequired(List.of("status"));
    problem.setDescription(
        "Problem Details for HTTP APIs (RFC 9457). Zusätzliche Erweiterungsfelder sind erlaubt.");
    problem.setAdditionalProperties(true);
    return problem;
  }

  // OAS 3.1 serializes `types`, OAS 3.0 `type`; set both.
  @SuppressWarnings("unchecked")
  private static <T> Schema<T> typed(String type) {
    return (Schema<T>) new Schema<T>().type(type).types(Set.of(type));
  }
}
