package ch.adeon.apps.docextract.shared.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;

/**
 * API contract doc/api/openapi.yaml: drift check against the spec springdoc generates from all
 * {@code @RestController}s (with application.yml), plus structural validation of the file.
 * Regenerate with {@code mvn test -Dtest=OpenApiContractTest -Dopenapi.write=true}.
 */
class OpenApiContractTest {

  private static final Path COMMITTED = Path.of("doc/api/openapi.yaml");
  private static final Path GENERATED = Path.of("target/openapi.generated.yaml");
  private static final String BASE_PACKAGE = "ch.adeon.apps.docextract";

  @Test
  void committed_spec_matches_the_generated_spec() throws Exception {
    String generated = generate();
    Files.createDirectories(GENERATED.getParent());
    Files.writeString(GENERATED, generated, UTF_8);
    if (Boolean.getBoolean("openapi.write")) {
      Files.writeString(COMMITTED, generated, UTF_8);
    }

    assertThat(Files.readString(COMMITTED, UTF_8))
        .as(
            "%s weicht vom generierten Vertrag ab (%s). Neu erzeugen: mvn test"
                + " -Dtest=OpenApiContractTest -Dopenapi.write=true",
            COMMITTED, GENERATED)
        .isEqualTo(generated);
  }

  @Test
  void committed_spec_is_a_valid_openapi_3_1_document() throws IOException {
    String yaml = Files.readString(COMMITTED, UTF_8);

    OpenAPI openApi = Yaml31.mapper().readValue(yaml, OpenAPI.class);
    assertThat(openApi.getOpenapi()).startsWith("3.1");
    assertThat(openApi.getPaths()).isNotEmpty();

    JsonNode root = Yaml31.mapper().readTree(yaml);
    List<String> unresolved = new ArrayList<>();
    collectRefs(root, new LinkedHashSet<>()).stream()
        .filter(ref -> !ref.startsWith("#/") || root.at(ref.substring(1)).isMissingNode())
        .forEach(unresolved::add);
    assertThat(unresolved).as("nicht auflösbare $ref").isEmpty();
    assertThat(root.at("/components/schemas/Problem/properties/status").isMissingNode()).isFalse();
  }

  @Test
  void every_error_response_is_problem_json_and_api_operations_declare_401() throws IOException {
    JsonNode root = Yaml31.mapper().readTree(Files.readString(COMMITTED, UTF_8));
    List<String> violations = new ArrayList<>();

    for (Map.Entry<String, JsonNode> path : root.get("paths").properties()) {
      for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
        String id = operation.getKey() + " " + path.getKey();
        JsonNode responses = operation.getValue().get("responses");
        if (path.getKey().startsWith("/api/") && !responses.has("401")) {
          violations.add(id + ": 401 fehlt");
        }
        for (Map.Entry<String, JsonNode> response : responses.properties()) {
          if (response.getKey().charAt(0) < '4') {
            continue;
          }
          JsonNode resolved = resolve(root, response.getValue());
          if (resolved.at("/content/application~1problem+json/schema/$ref").asText().isEmpty()) {
            violations.add(id + " " + response.getKey() + ": kein application/problem+json");
          }
        }
      }
    }
    assertThat(violations).isEmpty();
  }

  @Test
  void confirm_declares_idempotency_data_classification_and_slo() throws IOException {
    JsonNode root = Yaml31.mapper().readTree(Files.readString(COMMITTED, UTF_8));
    JsonNode confirm = root.at("/paths/~1api~1v1~1validation~1confirm/post");

    assertThat(confirm.at("/x-idempotency/key").asText()).isEqualTo("processId");
    assertThat(confirm.at("/x-idempotency/request-body-on-repeat").asText()).isNotBlank();
    assertThat(confirm.at("/x-data-classification/request").asText()).contains("personal-data");
    JsonNode slo = confirm.get("x-slo");
    for (String field :
        List.of(
            "/latency/percentile",
            "/latency/target",
            "/latency/window",
            "/latency/measuredAt",
            "/availability/target",
            "/availability/window",
            "/availability/definition",
            "/measurement")) {
      assertThat(slo.at(field).asText()).as("x-slo" + field).isNotBlank();
    }
    assertThat(slo.toString()).doesNotContain("OFFEN");
    assertThat(confirm.get("responses").properties())
        .extracting(Map.Entry::getKey)
        .contains("200", "400", "401", "404", "409", "410", "422", "502");
  }

  private static JsonNode resolve(JsonNode root, JsonNode node) {
    JsonNode ref = node.get("$ref");
    return ref == null ? node : root.at(ref.asText().substring(1));
  }

  private static Set<String> collectRefs(JsonNode node, Set<String> refs) {
    if (node.isObject()) {
      node.properties()
          .forEach(field -> {
            if (field.getKey().equals("$ref") && field.getValue().isTextual()) {
              refs.add(field.getValue().asText());
            } else {
              collectRefs(field.getValue(), refs);
            }
          });
    } else if (node.isArray()) {
      node.forEach(child -> collectRefs(child, refs));
    }
    return refs;
  }

  /** Boots only Web MVC + springdoc with every REST controller; their dependencies are mocks. */
  private static String generate() throws Exception {
    WebApplicationContextRunner runner =
        new WebApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("springdoc.api-docs.enabled=true")
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class,
                    WebMvcAutoConfiguration.class,
                    SpringDocConfiguration.class,
                    SpringDocConfigProperties.class,
                    SpringDocSpecPropertiesConfiguration.class,
                    SpringDocWebMvcConfiguration.class))
            .withUserConfiguration(OpenApiConfig.class);
    Set<Class<?>> dependencies = new LinkedHashSet<>();
    for (Class<?> controller : restControllers()) {
      runner = runner.withUserConfiguration(controller);
      for (Constructor<?> constructor : controller.getConstructors()) {
        dependencies.addAll(List.of(constructor.getParameterTypes()));
      }
    }
    for (Class<?> dependency : dependencies) {
      runner = registerMock(runner, dependency);
    }

    AtomicReference<String> spec = new AtomicReference<>();
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
      spec.set(
          mockMvc
              .perform(get("/v3/api-docs.yaml"))
              .andReturn()
              .getResponse()
              .getContentAsString(UTF_8));
    });
    return spec.get();
  }

  private static <T> WebApplicationContextRunner registerMock(
      WebApplicationContextRunner runner, Class<T> type) {
    return runner.withBean(type, () -> mock(type));
  }

  private static List<Class<?>> restControllers() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    List<Class<?>> controllers = new ArrayList<>();
    for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
      controllers.add(Class.forName(candidate.getBeanClassName()));
    }
    assertThat(controllers).isNotEmpty();
    controllers.sort(Comparator.comparing(Class::getName));
    return controllers;
  }
}
