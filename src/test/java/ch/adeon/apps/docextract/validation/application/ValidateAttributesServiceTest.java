package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessDocumentPort.StoredDocument;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.retrieval.application.DocumentTypeCandidatesPort;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDataType;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
import ch.adeon.apps.docextract.shared.error.InvalidRequestException;
import ch.adeon.apps.docextract.validation.port.DmsStoreValidationPort;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import ch.adeon.apps.docextract.validation.port.StoreValidationRequest;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ValidateAttributesServiceTest {

  private static final DvelopCredential CREDENTIAL =
      new DvelopCredential("Authorization", "Bearer t", null, "https://dms.local");

  private final DmsStoreValidationPort storeValidationPort = mock(DmsStoreValidationPort.class);
  private final DocumentTypeCandidatesPort candidatesPort = mock(DocumentTypeCandidatesPort.class);
  private final ProcessDocumentPort processDocumentPort = mock(ProcessDocumentPort.class);
  private final ProcessRepositoryPort processRepositoryPort = mock(ProcessRepositoryPort.class);
  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final ValidateAttributesService service =
      new ValidateAttributesService(
          storeValidationPort,
          candidatesPort,
          processDocumentPort,
          processRepositoryPort,
          processEventPort,
          "default-repo");

  private static DmsPropertyDefinition property(
      String id, boolean modifiable, boolean hidden, boolean system) {
    return new DmsPropertyDefinition(
        id,
        id,
        DmsPropertyDataType.ALPHANUMERIC,
        true,
        false,
        false,
        hidden,
        modifiable,
        system,
        false,
        false,
        0,
        0,
        List.of(),
        false);
  }

  private static CandidateAttribute attribute(String id, String value) {
    return new CandidateAttribute(id, value, null, null);
  }

  private static PropertyValue value(String id, String value) {
    return new PropertyValue(id, value, null);
  }

  @BeforeEach
  void stageProcess() {
    DmsDocumentType type =
        new DmsDocumentType(
            "DREC",
            "Rechnung",
            List.of(
                property("1", true, false, false),
                property("2", true, false, false),
                property("3", true, false, false),
                property("4", true, false, false),
                property("hidden", true, true, false),
                property("readonly", false, false, false),
                property("property_filename", true, false, true)));
    when(candidatesPort.find("tenant-a", "proc-1"))
        .thenReturn(Optional.of(List.of(new DmsDocumentMetadata(type, null, List.of()))));
    when(processDocumentPort.find("tenant-a", "proc-1"))
        .thenReturn(Optional.of(new StoredDocument("/blob/1", "invoice.pdf")));
    when(processRepositoryPort.retrieve("tenant-a", "proc-1")).thenReturn(Optional.of("repo-1"));
  }

  private void dmsAnswers(StoreValidationResult result) {
    when(storeValidationPort.validate(eq("tenant-a"), any(), eq(CREDENTIAL))).thenReturn(result);
  }

  private StoreValidationRequest sentRequest() {
    ArgumentCaptor<StoreValidationRequest> request =
        ArgumentCaptor.forClass(StoreValidationRequest.class);
    verify(storeValidationPort).validate(eq("tenant-a"), request.capture(), eq(CREDENTIAL));
    return request.getValue();
  }

  @Test
  void empty_mandatory_fields_do_not_prevent_the_dms_call() {
    dmsAnswers(new StoreValidationResult.Valid(List.of(), List.of()));

    ValidationReport report = service.validate("tenant-a", "proc-1", "DREC", List.of(), CREDENTIAL);

    assertThat(report.valid()).isTrue();
    StoreValidationRequest request = sentRequest();
    assertThat(request.properties()).isEmpty();
    assertThat(request.objectDefinitionId()).isEqualTo("DREC");
    assertThat(request.repositoryId()).isEqualTo("repo-1");
    assertThat(request.filename()).isEqualTo("invoice.pdf");
  }

  @Test
  void a_missing_repository_falls_back_to_the_configured_one() {
    when(processRepositoryPort.retrieve("tenant-a", "proc-1")).thenReturn(Optional.empty());
    dmsAnswers(new StoreValidationResult.Valid(List.of(), List.of()));

    service.validate("tenant-a", "proc-1", "DREC", List.of(), CREDENTIAL);

    assertThat(sentRequest().repositoryId()).isEqualTo("default-repo");
  }

  @Test
  void only_writable_non_system_properties_of_the_type_are_sent() {
    dmsAnswers(new StoreValidationResult.Valid(List.of(), List.of()));

    service.validate(
        "tenant-a",
        "proc-1",
        "DREC",
        List.of(
            attribute("1", "a"),
            attribute("hidden", "b"),
            attribute("readonly", "c"),
            attribute("property_filename", "d"),
            attribute("unknown", "e")),
        CREDENTIAL);

    StoreValidationRequest request = sentRequest();
    assertThat(request.properties()).containsExactly(value("1", "a"));
    assertThat(request.definitions()).containsOnlyKeys("1", "2", "3", "4");
  }

  @Test
  void valid_adopts_the_properties_with_their_origin() {
    // sent: 1 unchanged, 2 normalised by the DMS, 3 emptied by the DMS; 4 not sent but set by a
    // hook
    dmsAnswers(
        new StoreValidationResult.Valid(
            List.of(value("1", "same"), value("2", "12"), value("3", "kept")),
            List.of(value("1", "same"), value("2", "12.00"), value("4", "In Prüfung"))));

    ValidationReport report =
        service.validate(
            "tenant-a",
            "proc-1",
            "DREC",
            List.of(attribute("1", "same"), attribute("2", "12"), attribute("3", "kept")),
            CREDENTIAL);

    assertThat(report.valid()).isTrue();
    assertThat(report.error()).isNull();
    assertThat(report.attributes())
        .containsExactly(
            new ValidatedAttribute("1", "same", null, AttributeOrigin.LLM, false),
            new ValidatedAttribute("2", "12.00", null, AttributeOrigin.LLM, true),
            new ValidatedAttribute("3", "kept", null, AttributeOrigin.LLM, false),
            new ValidatedAttribute("4", "In Prüfung", null, AttributeOrigin.DMS, true));
  }

  @Test
  void a_value_that_only_differs_before_normalisation_is_not_a_change_by_the_dms() {
    // the adapter submitted the normalised "12.50", the DMS answers the same
    dmsAnswers(
        new StoreValidationResult.Valid(
            List.of(value("1", "12.50")), List.of(value("1", "12.50"))));

    ValidationReport report =
        service.validate(
            "tenant-a", "proc-1", "DREC", List.of(attribute("1", "12,50")), CREDENTIAL);

    assertThat(report.attributes())
        .containsExactly(new ValidatedAttribute("1", "12.50", null, AttributeOrigin.LLM, false));
  }

  @Test
  void an_empty_answer_never_deletes_a_suggestion() {
    dmsAnswers(new StoreValidationResult.Valid(List.of(value("1", "x")), List.of()));

    ValidationReport report =
        service.validate("tenant-a", "proc-1", "DREC", List.of(attribute("1", "x")), CREDENTIAL);

    assertThat(report.attributes())
        .containsExactly(new ValidatedAttribute("1", "x", null, AttributeOrigin.LLM, false));
  }

  @Test
  void multi_values_are_compared_in_order() {
    dmsAnswers(
        new StoreValidationResult.Valid(
            List.of(new PropertyValue("1", null, List.of("a", "b"))),
            List.of(new PropertyValue("1", null, List.of("b", "a")))));

    ValidationReport report =
        service.validate(
            "tenant-a",
            "proc-1",
            "DREC",
            List.of(new CandidateAttribute("1", null, List.of("a", "b"), null)),
            CREDENTIAL);

    assertThat(report.attributes())
        .containsExactly(
            new ValidatedAttribute("1", null, List.of("b", "a"), AttributeOrigin.LLM, true));
  }

  @Test
  void an_earlier_dms_origin_stays_when_the_value_is_unchanged() {
    dmsAnswers(
        new StoreValidationResult.Valid(List.of(value("4", "Hook")), List.of(value("4", "Hook"))));

    ValidationReport report =
        service.validate(
            "tenant-a",
            "proc-1",
            "DREC",
            List.of(new CandidateAttribute("4", "Hook", null, AttributeOrigin.DMS)),
            CREDENTIAL);

    assertThat(report.attributes())
        .containsExactly(new ValidatedAttribute("4", "Hook", null, AttributeOrigin.DMS, false));
  }

  @Test
  void only_allowlisted_properties_are_taken_over_from_the_answer() {
    dmsAnswers(
        new StoreValidationResult.Valid(
            List.of(),
            List.of(value("property_filename", "x.pdf"), value("hidden", "h"), value("4", "ok"))));

    ValidationReport report = service.validate("tenant-a", "proc-1", "DREC", List.of(), CREDENTIAL);

    assertThat(report.attributes())
        .containsExactly(new ValidatedAttribute("4", "ok", null, AttributeOrigin.DMS, true));
  }

  @Test
  void invalid_reports_the_one_error_and_no_attributes() {
    StoreValidationError error = new StoreValidationError("1", "0010019", "Pflichtfeld fehlt");
    dmsAnswers(new StoreValidationResult.Invalid(error));

    ValidationReport report = service.validate("tenant-a", "proc-1", "DREC", List.of(), CREDENTIAL);

    assertThat(report.valid()).isFalse();
    assertThat(report.attributes()).isNull();
    assertThat(report.error()).isEqualTo(error);
  }

  @Test
  void another_tenants_process_is_not_found_and_the_dms_is_never_called() {
    when(candidatesPort.find("tenant-b", "proc-1")).thenReturn(Optional.empty());
    when(processEventPort.exists("tenant-b", "proc-1")).thenReturn(false);

    assertThatThrownBy(() -> service.validate("tenant-b", "proc-1", "DREC", List.of(), CREDENTIAL))
        .isInstanceOf(ProcessNotFoundException.class);

    verify(storeValidationPort, never()).validate(any(), any(), any());
  }

  @Test
  void a_document_type_that_is_no_candidate_of_the_process_is_a_bad_request() {
    when(processEventPort.exists("tenant-a", "proc-1")).thenReturn(true);

    assertThatThrownBy(() -> service.validate("tenant-a", "proc-1", "OTHER", List.of(), CREDENTIAL))
        .isInstanceOf(InvalidRequestException.class);

    verify(storeValidationPort, never()).validate(any(), any(), any());
  }

  @Test
  void an_unavailable_dms_is_passed_on_so_the_caller_can_answer_502() {
    when(storeValidationPort.validate(any(), any(), any()))
        .thenThrow(new AdapterUnavailableException("storevalidate answered HTTP 503"));

    assertThatThrownBy(() -> service.validate("tenant-a", "proc-1", "DREC", List.of(), CREDENTIAL))
        .isInstanceOf(AdapterUnavailableException.class);
  }
}
