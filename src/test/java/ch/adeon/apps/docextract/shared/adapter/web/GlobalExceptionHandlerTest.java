package ch.adeon.apps.docextract.shared.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.adeon.apps.docextract.ingest.application.BlobNotFoundException;
import ch.adeon.apps.docextract.ingest.application.DmsChunkUploadException;
import ch.adeon.apps.docextract.ingest.application.DmsRepositoryException;
import ch.adeon.apps.docextract.ingest.application.PreviewRenderingException;
import ch.adeon.apps.docextract.ingest.application.UploadRejectedException;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.adapter.out.dms.DmsObjectDefinitionException;
import ch.adeon.apps.docextract.retrieval.adapter.out.dms.ValueListWebhookException;
import ch.adeon.apps.docextract.shared.error.ContentException;
import ch.adeon.apps.docextract.shared.error.ExtractionException;
import ch.adeon.apps.docextract.shared.error.LimitExceededException;
import ch.adeon.apps.docextract.shared.error.LimitExceededException.Limit;
import ch.adeon.apps.docextract.shared.error.RangeNotSatisfiableException;
import ch.adeon.apps.docextract.shared.error.RetrievalException;
import ch.adeon.apps.docextract.validation.application.AlreadyFinalizedException;
import ch.adeon.apps.docextract.validation.application.InvalidAttributesException;
import ch.adeon.apps.docextract.validation.application.PossiblyAlreadyFinalizedException;
import ch.adeon.apps.docextract.validation.application.ProcessExpiredException;
import ch.adeon.apps.docextract.validation.port.DmsWriteFailedException;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** ProblemDetail mapping of {@link GlobalExceptionHandler}: status + type per error class. */
class GlobalExceptionHandlerTest {

  private static final String PROCESS_ID = "3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b";
  private static final String TYPE = "urn:docextract:problem:";
  // Values that must never show up in a response body (C-4).
  private static final String FILE_NAME = "Rechnung-Muster-AG.pdf";
  private static final String ATTRIBUTE_VALUE = "CH93 0076 2011 6238 5295 7";

  static Supplier<RuntimeException> failure;

  @RestController
  static class FailingController {

    @GetMapping("/fail")
    String fail() {
      throw failure.get();
    }

    @GetMapping("/blob/{id}")
    String blob(@PathVariable UUID id) {
      return id.toString();
    }

    @GetMapping("/param")
    String param(@RequestParam("name") String name) {
      return name;
    }
  }

  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(new FailingController())
          .setControllerAdvice(new GlobalExceptionHandler())
          .build();

  static Stream<Arguments> mappings() {
    return Stream.of(
        row(() -> new UploadRejectedException("File must not be empty"), 400, "invalid-request"),
        row(() -> new ProcessNotFoundException(PROCESS_ID), 404, "not-found"),
        row(() -> new BlobNotFoundException(UUID.randomUUID()), 404, "not-found"),
        row(() -> new AlreadyFinalizedException(PROCESS_ID), 409, "conflict-state"),
        row(() -> new PossiblyAlreadyFinalizedException(PROCESS_ID), 409, "conflict-state"),
        row(() -> new ProcessExpiredException(PROCESS_ID), 410, "process-expired"),
        row(() -> new LimitExceededException(Limit.FILE_SIZE, "too large"), 413, "limit-exceeded"),
        row(() -> new LimitExceededException(Limit.PAGE_COUNT, "too many"), 422, "limit-exceeded"),
        row(() -> new InvalidAttributesException(PROCESS_ID), 422, "invalid-attributes"),
        row(() -> new RangeNotSatisfiableException(10), 416, "range-not-satisfiable"),
        row(
            () -> new DmsRepositoryException("x", new RuntimeException()),
            502,
            "adapter-unavailable"),
        row(() -> new DmsChunkUploadException("x"), 502, "adapter-unavailable"),
        row(() -> new PreviewRenderingException("x"), 502, "adapter-unavailable"),
        row(
            () -> new DmsWriteFailedException("x", new RuntimeException()),
            502,
            "adapter-unavailable"),
        row(
            () -> new DmsObjectDefinitionException("x", new RuntimeException()),
            502,
            "adapter-unavailable"),
        row(
            () -> new ValueListWebhookException("x", new RuntimeException()),
            502,
            "adapter-unavailable"),
        row(() -> new ExtractionException("x"), 500, "internal-error"),
        row(() -> new RetrievalException("x"), 500, "internal-error"),
        row(() -> new ContentException("x"), 500, "internal-error"),
        row(() -> new IllegalStateException("unexpected"), 500, "internal-error"));
  }

  private static Arguments row(Supplier<RuntimeException> exception, int status, String code) {
    return Arguments.of(exception, status, code);
  }

  @ParameterizedTest
  @MethodSource("mappings")
  void each_error_class_maps_to_its_status_and_type(
      Supplier<RuntimeException> exception, int status, String code) throws Exception {
    failure = exception;

    mockMvc
        .perform(get("/fail"))
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(TYPE + code))
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.title").isNotEmpty())
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  void process_id_is_an_extension_member_when_known() throws Exception {
    failure = () -> new ProcessNotFoundException(PROCESS_ID);

    mockMvc.perform(get("/fail")).andExpect(jsonPath("$.processId").value(PROCESS_ID));
  }

  @Test
  void unsatisfiable_range_states_the_length_in_content_range() throws Exception {
    failure = () -> new RangeNotSatisfiableException(1234);

    mockMvc.perform(get("/fail")).andExpect(header().string("Content-Range", "bytes */1234"));
  }

  @Test
  void problem_is_also_returned_to_a_client_that_only_accepts_event_streams() throws Exception {
    failure = () -> new ProcessNotFoundException(PROCESS_ID);

    mockMvc.perform(get("/fail").accept("text/event-stream")).andExpect(status().isNotFound());
  }

  static Stream<Supplier<RuntimeException>> piiCarryingFailures() {
    return Stream.of(
        () -> new InvalidAttributesException(PROCESS_ID, "IBAN " + ATTRIBUTE_VALUE + " is invalid"),
        () -> new DmsChunkUploadException("rejected " + FILE_NAME),
        () -> new PreviewRenderingException("file conversion failed for " + FILE_NAME),
        () -> new DmsWriteFailedException(ATTRIBUTE_VALUE, new RuntimeException(FILE_NAME)),
        () -> new ExtractionException("model said " + ATTRIBUTE_VALUE),
        () -> new ContentException("cannot read " + FILE_NAME),
        () -> new IllegalStateException(FILE_NAME + " " + ATTRIBUTE_VALUE));
  }

  @ParameterizedTest
  @MethodSource("piiCarryingFailures")
  void detail_never_contains_attribute_values_or_file_names(Supplier<RuntimeException> exception)
      throws Exception {
    failure = exception;

    String body = mockMvc.perform(get("/fail")).andReturn().getResponse().getContentAsString();

    assertThat(body).isNotEmpty().doesNotContain(FILE_NAME).doesNotContain(ATTRIBUTE_VALUE);
  }

  @Test
  void framework_errors_are_problem_details_with_a_docextract_type() throws Exception {
    mockMvc
        .perform(get("/param"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(TYPE + "invalid-request"));
    mockMvc
        .perform(post("/fail"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.type").value(TYPE + "invalid-request"));
  }

  @Test
  void rejected_path_variable_value_is_not_echoed_in_detail() throws Exception {
    mockMvc
        .perform(get("/blob/secret-not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(TYPE + "invalid-request"))
        .andExpect(jsonPath("$.detail").value(not(containsString("secret-"))));
  }

  @Test
  void multipart_limit_maps_to_413_with_the_same_type_as_the_file_size_limit() throws Exception {
    failure = () -> new MaxUploadSizeExceededException(50L * 1024 * 1024);

    mockMvc
        .perform(get("/fail"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.type").value(TYPE + "limit-exceeded"));
  }

  @Test
  void security_exceptions_are_left_to_the_security_filter_chain() {
    failure = () -> new AccessDeniedException("denied");

    assertThatThrownBy(() -> mockMvc.perform(get("/fail")))
        .hasRootCauseInstanceOf(AccessDeniedException.class);
  }
}
