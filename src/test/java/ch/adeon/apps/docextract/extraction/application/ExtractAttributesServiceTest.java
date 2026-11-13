package ch.adeon.apps.docextract.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import ch.adeon.apps.docextract.content.application.ContentStagingPort;
import ch.adeon.apps.docextract.content.domain.Representation;
import ch.adeon.apps.docextract.content.port.DocumentContent;
import ch.adeon.apps.docextract.extraction.domain.LlmResponse;
import ch.adeon.apps.docextract.extraction.domain.LlmTool;
import ch.adeon.apps.docextract.extraction.domain.ModelInfo;
import ch.adeon.apps.docextract.extraction.port.LlmPort;
import ch.adeon.apps.docextract.process.application.ProcessEvent;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.process.application.ProcessStep;
import ch.adeon.apps.docextract.process.application.StepStatus;
import ch.adeon.apps.docextract.retrieval.application.DocumentTypeCandidatesPort;
import ch.adeon.apps.docextract.retrieval.application.RetrievalResultPort;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.port.DmsProperty;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.shared.error.ExtractionException;
import ch.adeon.apps.docextract.validation.application.AttributeOrigin;
import ch.adeon.apps.docextract.validation.application.ValidateAttributes;
import ch.adeon.apps.docextract.validation.application.ValidatedAttribute;
import ch.adeon.apps.docextract.validation.application.ValidationReport;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ExtractAttributesServiceTest {

  private static final DvelopCredential CREDENTIAL =
      new DvelopCredential("Authorization", "Bearer t", null, "https://dms.local");
  private static final ModelInfo MODEL = new ModelInfo("openai-compatible", "qwen3");

  private final DocumentTypeCandidatesPort candidatesPort = mock(DocumentTypeCandidatesPort.class);
  private final ContentStagingPort contentStagingPort = mock(ContentStagingPort.class);
  private final ExtractionResultPort extractionResultPort = mock(ExtractionResultPort.class);
  private final LlmPort llmPort = mock(LlmPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final AuditPort auditPort = mock(AuditPort.class);
  private final ValidateAttributes validateAttributes = mock(ValidateAttributes.class);
  private final RetrievalResultPort retrievalResultPort = mock(RetrievalResultPort.class);
  private final ExtractAttributesService service = service(10, 5);

  private ExtractAttributesService service(int maxToolCalls, int maxValidationRounds) {
    return new ExtractAttributesService(
        candidatesPort,
        contentStagingPort,
        mock(ValueListPort.class),
        validateAttributes,
        extractionResultPort,
        retrievalResultPort,
        llmPort,
        processEventPort,
        auditPort,
        JsonMapper.builder().build(),
        mock(ProcessRepositoryPort.class),
        "repo-1",
        50,
        maxToolCalls,
        maxValidationRounds,
        0.5,
        1);
  }

  @BeforeEach
  void dmsAcceptsEverythingUnchanged() {
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(ValidationReport.valid(List.of()));
  }

  private static DmsPropertyDefinition writable(String id) {
    return new DmsPropertyDefinition(
        id,
        id,
        DmsPropertyDataType.ALPHANUMERIC,
        false,
        false,
        false,
        false,
        true,
        false,
        false,
        false,
        0,
        0,
        List.of(),
        false);
  }

  private static DmsDocumentMetadata candidate(String typeId) {
    return new DmsDocumentMetadata(
        new DmsDocumentType(typeId, typeId, List.of(writable("p1"))), null, List.of());
  }

  @Test
  void llm_events_carry_the_tenant_of_the_pipeline_and_the_model() {
    when(candidatesPort.find("tenant-a", "proc-1"))
        .thenReturn(Optional.of(List.of(candidate("DREC"), candidate("DLET"))));
    when(contentStagingPort.retrieve("proc-1"))
        .thenReturn(
            Optional.of(new DocumentContent(Representation.FULLTEXT, "Rechnung Muster AG")));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(new LlmResponse("{\"documentTypeId\":\"DREC\"}", MODEL, 11))
        .thenReturn(
            new LlmResponse(
                "{\"attributes\":[{\"propertyId\":\"p1\",\"value\":null,\"values\":null,"
                    + "\"confidence\":0.5,\"sourceExcerpt\":null}]}",
                MODEL,
                22));

    service.extract("tenant-a", "proc-1", CREDENTIAL);

    ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditPort, org.mockito.Mockito.times(2)).append(events.capture());
    assertThat(events.getAllValues())
        .extracting(AuditEvent::eventType)
        .containsExactly("extraction.classify", "extraction.suggest-attributes");
    assertThat(events.getAllValues())
        .allSatisfy(event -> {
          assertThat(event.tenantId()).isEqualTo("tenant-a");
          assertThat(event.channel()).isEqualTo(AuditChannel.SYSTEM);
          assertThat(event.provider()).isEqualTo("openai-compatible");
          assertThat(event.modelId()).isEqualTo("qwen3");
          assertThat(event.argsHash()).hasSize(64).doesNotContain("Muster");
        });
    assertThat(events.getAllValues()).extracting(AuditEvent::tokenCount).containsExactly(11, 22);
  }

  @Test
  void invalid_llm_json_is_retried_then_ends_in_a_controlled_failure() {
    when(candidatesPort.find("tenant-a", "proc-1"))
        .thenReturn(Optional.of(List.of(candidate("DREC"), candidate("DLET"))));
    when(contentStagingPort.retrieve("proc-1"))
        .thenReturn(
            Optional.of(new DocumentContent(Representation.FULLTEXT, "Rechnung Muster AG")));
    // Neither attempt (first call + the one configured retry) is valid JSON for ClassifyResponse.
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(new LlmResponse("not json", MODEL, 11));

    assertThatThrownBy(() -> service.extract("tenant-a", "proc-1", CREDENTIAL))
        .isInstanceOf(ExtractionException.class)
        .hasCauseInstanceOf(LlmException.class);

    verify(llmPort, org.mockito.Mockito.times(2))
        .generate(anyString(), anyString(), anyMap(), anyList());
    verify(auditPort, org.mockito.Mockito.times(2)).append(any());
    verify(extractionResultPort, never()).store(any(), any(), any());
    verify(processEventPort)
        .publish(
            "tenant-a", new ProcessEvent("proc-1", ProcessStep.EXTRACTED, StepStatus.FAILED, null));
  }

  @Test
  void llm_suggested_value_outside_the_known_value_list_is_rejected_as_unknown() {
    DmsPropertyDefinition withValueList =
        new DmsPropertyDefinition(
            "p1",
            "p1",
            DmsPropertyDataType.ALPHANUMERIC,
            false,
            true,
            false,
            false,
            true,
            false,
            false,
            false,
            0,
            0,
            List.of("A", "B"),
            false);
    DmsDocumentMetadata singleCandidate =
        new DmsDocumentMetadata(
            new DmsDocumentType("DREC", "DREC", List.of(withValueList)), null, List.of());
    when(candidatesPort.find("tenant-a", "proc-1"))
        .thenReturn(Optional.of(List.of(singleCandidate)));
    when(contentStagingPort.retrieve("proc-1"))
        .thenReturn(
            Optional.of(new DocumentContent(Representation.FULLTEXT, "Rechnung Muster AG")));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(
            new LlmResponse(
                "{\"attributes\":[{\"propertyId\":\"p1\",\"value\":\"ZZZ\",\"values\":null,"
                    + "\"confidence\":0.9,\"sourceExcerpt\":null}]}",
                MODEL,
                22));

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .containsExactly(new ExtractedAttribute("p1", null, null, 0.0, null));
  }

  @Test
  void retry_of_an_unknown_or_foreign_process_is_not_found_and_does_nothing() {
    when(candidatesPort.find("tenant-b", "proc-1")).thenReturn(Optional.empty());
    when(processEventPort.exists("tenant-b", "proc-1")).thenReturn(false);

    assertThatThrownBy(() -> service.retry("tenant-b", "proc-1", "DREC", CREDENTIAL))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(processEventPort, never()).publish(any(), any());
    verify(llmPort, never()).generate(any(), any(), any());
    verify(extractionResultPort, never()).store(any(), any(), any());
  }

  @Test
  void retry_of_an_own_process_without_candidates_is_an_extraction_error_not_a_404() {
    when(candidatesPort.find("tenant-a", "proc-1")).thenReturn(Optional.empty());
    when(processEventPort.exists("tenant-a", "proc-1")).thenReturn(true);

    assertThatThrownBy(() -> service.retry("tenant-a", "proc-1", "DREC", CREDENTIAL))
        .isNotInstanceOf(ProcessNotFoundException.class);
  }

  // --- ADR-016: validate_attributes tool, correction loop, final DMS check ---

  private static final String SUGGEST_RIGHT =
      "{\"attributes\":[{\"propertyId\":\"p1\",\"value\":\"right\",\"values\":null,"
          + "\"confidence\":0.9,\"sourceExcerpt\":\"ex\"}]}";
  private static final String SUGGEST_WRONG = SUGGEST_RIGHT.replace("right", "wrong");

  private static String validateArgs(String value) {
    return "{\"attributes\":[{\"propertyId\":\"p1\",\"value\":\"%s\",\"values\":null}]}"
        .formatted(value);
  }

  private void stageTwoPropertyType() {
    DmsDocumentMetadata type =
        new DmsDocumentMetadata(
            new DmsDocumentType("DREC", "DREC", List.of(writable("p1"), writable("p2"))),
            null,
            List.of());
    when(candidatesPort.find("tenant-a", "proc-1")).thenReturn(Optional.of(List.of(type)));
    when(contentStagingPort.retrieve("proc-1"))
        .thenReturn(
            Optional.of(new DocumentContent(Representation.FULLTEXT, "Rechnung Muster AG")));
  }

  /** The LLM calls validate_attributes with each of {@code toolArguments}, then answers. */
  private List<String> llmValidatesThenAnswers(List<String> toolArguments, String finalJson) {
    List<String> toolResults = new java.util.ArrayList<>();
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenAnswer(invocation -> {
          List<LlmTool> tools = invocation.getArgument(3);
          LlmTool validate =
              tools.stream()
                  .filter(tool -> tool.name().equals("validate_attributes"))
                  .findFirst()
                  .orElseThrow();
          toolArguments.forEach(arguments -> toolResults.add(validate.handler().apply(arguments)));
          return new LlmResponse(finalJson, MODEL, 20);
        });
    return toolResults;
  }

  private ExtractionResult storedResult() {
    ArgumentCaptor<ExtractionResult> stored = ArgumentCaptor.forClass(ExtractionResult.class);
    verify(extractionResultPort).store(eq("tenant-a"), eq("proc-1"), stored.capture());
    return stored.getValue();
  }

  @Test
  void the_validation_tool_is_offered_as_soon_as_the_type_is_chosen_even_with_empty_fields() {
    stageTwoPropertyType();
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(new LlmResponse("{\"attributes\":[]}", MODEL, 5));

    service.extract("tenant-a", "proc-1", CREDENTIAL);

    ArgumentCaptor<List<LlmTool>> tools = ArgumentCaptor.forClass(List.class);
    verify(llmPort).generate(anyString(), anyString(), anyMap(), tools.capture());
    assertThat(tools.getValue()).extracting(LlmTool::name).containsExactly("validate_attributes");
    // the final check runs although nothing was extracted: hooks may set mandatory fields
    verify(validateAttributes).validate("tenant-a", "proc-1", "DREC", List.of(), CREDENTIAL);
  }

  @Test
  void a_field_corrected_after_a_dms_error_is_capped_and_marked_and_hook_values_are_adopted() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), eq("proc-1"), eq("DREC"), anyList(), any()))
        // tool call 1: the DMS rejects p1 = "wrong"
        .thenReturn(
            ValidationReport.invalid(new StoreValidationError("p1", "0010019", "Pflichtfeld")))
        // tool call 2: p1 = "right" is accepted
        .thenReturn(ValidationReport.valid(List.of()))
        // final check
        .thenReturn(
            ValidationReport.valid(
                List.of(
                    new ValidatedAttribute("p1", "right", null, AttributeOrigin.LLM, false),
                    new ValidatedAttribute("p2", "Hook", null, AttributeOrigin.DMS, true))));
    List<String> toolResults =
        llmValidatesThenAnswers(
            List.of(validateArgs("wrong"), validateArgs("right")), SUGGEST_RIGHT);

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(toolResults.get(0)).contains("\"valid\":false").contains("Pflichtfeld");
    assertThat(toolResults.get(1)).contains("\"valid\":true");
    assertThat(result.validation()).isEqualTo(ValidationOutcome.VALID);
    assertThat(result.attributes())
        .containsExactly(
            new ExtractedAttribute(
                "p1", "right", null, 0.5, "ex", AttributeOrigin.LLM, false, true),
            new ExtractedAttribute(
                "p2", "Hook", null, null, null, AttributeOrigin.DMS, true, false));
    assertThat(storedResult()).isEqualTo(result);
  }

  @Test
  void a_field_the_llm_left_unchanged_after_a_dms_error_is_not_devalued() {
    stageTwoPropertyType();
    StoreValidationError error = new StoreValidationError("p1", "0010019", "Pflichtfeld");
    when(validateAttributes.validate(any(), eq("proc-1"), eq("DREC"), anyList(), any()))
        .thenReturn(ValidationReport.invalid(error));
    llmValidatesThenAnswers(List.of(validateArgs("wrong")), SUGGEST_WRONG);

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .containsExactly(new ExtractedAttribute("p1", "wrong", null, 0.9, "ex"));
    assertThat(result.validation()).isEqualTo(ValidationOutcome.invalid(error));
  }

  @Test
  void a_corrected_field_with_a_lower_confidence_keeps_it() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), eq("proc-1"), eq("DREC"), anyList(), any()))
        .thenReturn(ValidationReport.invalid(new StoreValidationError("p1", "1", "m")))
        .thenReturn(ValidationReport.valid(List.of()));
    llmValidatesThenAnswers(List.of(validateArgs("wrong")), SUGGEST_RIGHT.replace("0.9", "0.3"));

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes().get(0).confidence()).isEqualTo(0.3);
    assertThat(result.attributes().get(0).correctedAfterValidation()).isTrue();
  }

  @Test
  void an_error_without_a_property_id_devalues_nothing() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), eq("proc-1"), eq("DREC"), anyList(), any()))
        .thenReturn(ValidationReport.invalid(new StoreValidationError(null, "1", "m")));
    llmValidatesThenAnswers(List.of(validateArgs("wrong")), SUGGEST_RIGHT);

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .containsExactly(new ExtractedAttribute("p1", "right", null, 0.9, "ex"));
  }

  @Test
  void the_validation_round_limit_stops_the_loop() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), eq("proc-1"), eq("DREC"), anyList(), any()))
        .thenReturn(ValidationReport.invalid(new StoreValidationError("p1", "1", "m")));
    List<String> toolResults =
        llmValidatesThenAnswers(
            List.of(validateArgs("a"), validateArgs("b"), validateArgs("c")), SUGGEST_RIGHT);

    ExtractionResult result = service(10, 2).extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(toolResults.get(0)).contains("\"valid\":false");
    assertThat(toolResults.get(1)).contains("\"valid\":false");
    assertThat(toolResults.get(2)).contains("validation round limit reached");
    // two tool checks plus the final check
    verify(validateAttributes, org.mockito.Mockito.times(3))
        .validate(any(), any(), any(), anyList(), any());
    assertThat(result.validation().status()).isEqualTo(ValidationOutcome.Status.INVALID);
  }

  @Test
  void max_tool_calls_applies_to_the_validation_tool_too() {
    stageTwoPropertyType();
    List<String> toolResults =
        llmValidatesThenAnswers(List.of(validateArgs("a"), validateArgs("b")), SUGGEST_RIGHT);

    service(1, 5).extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(toolResults.get(0)).contains("\"valid\":true");
    assertThat(toolResults.get(1)).contains("tool call limit reached");
  }

  @Test
  void an_unreachable_dms_leaves_the_suggestions_and_the_status_not_checked() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenThrow(new AdapterUnavailableException("storevalidate answered HTTP 503"));
    llmValidatesThenAnswers(List.of(validateArgs("right")), SUGGEST_RIGHT);

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.validation()).isEqualTo(ValidationOutcome.NOT_CHECKED);
    assertThat(result.attributes())
        .containsExactly(new ExtractedAttribute("p1", "right", null, 0.9, "ex"));
    verify(processEventPort)
        .publish(
            "tenant-a",
            new ProcessEvent("proc-1", ProcessStep.EXTRACTED, StepStatus.COMPLETED, null));
  }

  @Test
  void a_hook_value_for_a_property_the_llm_left_empty_replaces_the_empty_attribute() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(
            ValidationReport.valid(
                List.of(new ValidatedAttribute("p1", "Hook", null, AttributeOrigin.DMS, true))));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(
            new LlmResponse(
                "{\"attributes\":[{\"propertyId\":\"p1\",\"value\":null,\"values\":null,"
                    + "\"confidence\":0,\"sourceExcerpt\":null}]}",
                MODEL,
                5));

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .containsExactly(
            new ExtractedAttribute(
                "p1", "Hook", null, null, null, AttributeOrigin.DMS, true, false));
  }

  @Test
  void a_value_the_dms_normalised_is_taken_over_and_flagged_but_keeps_confidence_and_excerpt() {
    stageTwoPropertyType();
    when(validateAttributes.validate(any(), any(), any(), anyList(), any()))
        .thenReturn(
            ValidationReport.valid(
                List.of(new ValidatedAttribute("p1", "12.00", null, AttributeOrigin.LLM, true))));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(new LlmResponse(SUGGEST_RIGHT.replace("right", "12"), MODEL, 5));

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .containsExactly(
            new ExtractedAttribute(
                "p1", "12.00", null, 0.9, "ex", AttributeOrigin.LLM, true, false));
  }

  private String similarHit(String p1, String p2) {
    DmsDocumentMetadata metadata =
        new DmsDocumentMetadata(
            new DmsDocumentType("DREC", "DREC", List.of(writable("p1"), writable("p2"))),
            null,
            List.of(new DmsProperty("p1", p1, null), new DmsProperty("p2", p2, null)));
    return JsonMapper.builder().build().writeValueAsString(metadata);
  }

  @Test
  void empty_properties_are_proposed_from_similar_documents_with_a_confidence_below_the_review_threshold() {
    stageTwoPropertyType();
    when(retrievalResultPort.find("tenant-a", "proc-1"))
        .thenReturn(
            Optional.of(
                List.of(
                    new RetrievalResult("r", "A1", 1.0, similarHit("In Prüfung", "Bau")),
                    new RetrievalResult("r", "A2", 0.9, similarHit("In Prüfung", "Bau")),
                    new RetrievalResult("r", "A3", 0.8, similarHit("Freigabe", "Bau")))));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(new LlmResponse("{\"attributes\":[]}", MODEL, 5));

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .containsExactly(
            new ExtractedAttribute(
                "p1", "In Prüfung", null, 0.96 * 2 / 3, "2 von 3 ähnlichen Dokumenten",
                AttributeOrigin.SIMILAR, false, false),
            new ExtractedAttribute(
                "p2", "Bau", null, 0.96, "3 von 3 ähnlichen Dokumenten",
                AttributeOrigin.SIMILAR, false, false));
  }

  @Test
  void the_maximum_confidence_of_a_similar_proposal_needs_the_maximum_number_of_documents() {
    stageTwoPropertyType();
    when(retrievalResultPort.find("tenant-a", "proc-1"))
        .thenReturn(
            Optional.of(List.of(new RetrievalResult("r", "A1", 1.0, similarHit("Freigabe", "Bau")))));
    when(llmPort.generate(anyString(), anyString(), anyMap(), anyList()))
        .thenReturn(new LlmResponse("{\"attributes\":[]}", MODEL, 5));

    ExtractionResult result = service.extract("tenant-a", "proc-1", CREDENTIAL);

    assertThat(result.attributes())
        .extracting(ExtractedAttribute::confidence)
        .containsExactly(0.96 / 3, 0.96 / 3);
  }
}
