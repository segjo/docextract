package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.process.application.ProcessDocumentPort;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.process.application.ProcessRepositoryPort;
import ch.adeon.apps.docextract.retrieval.application.DocumentTypeCandidatesPort;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import ch.adeon.apps.docextract.security.application.DvelopCredential;
import ch.adeon.apps.docextract.shared.error.InvalidRequestException;
import ch.adeon.apps.docextract.validation.port.DmsStoreValidationPort;
import ch.adeon.apps.docextract.validation.port.PropertyValue;
import ch.adeon.apps.docextract.validation.port.StoreValidationRequest;
import ch.adeon.apps.docextract.validation.port.StoreValidationResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Read-only DMS check of a process's attribute values (ADR-016). There is no local mandatory field
 * check: hooks in the DMS may set those, only the DMS decides. The allowlist (writable non-system
 * properties of the chosen document type) filters both the request and what is adopted from the
 * answer. A valid answer is compared with what was sent, after normalisation, to tell where each
 * value comes from.
 */
@Service
public class ValidateAttributesService implements ValidateAttributes {

  private final DmsStoreValidationPort storeValidationPort;
  private final DocumentTypeCandidatesPort documentTypeCandidatesPort;
  private final ProcessDocumentPort processDocumentPort;
  private final ProcessRepositoryPort processRepositoryPort;
  private final ProcessEventPort processEventPort;
  private final String defaultRepositoryId;

  public ValidateAttributesService(
      DmsStoreValidationPort storeValidationPort,
      DocumentTypeCandidatesPort documentTypeCandidatesPort,
      ProcessDocumentPort processDocumentPort,
      ProcessRepositoryPort processRepositoryPort,
      ProcessEventPort processEventPort,
      @Value("${docextract.ingest.dms.repository-id}") String defaultRepositoryId) {
    this.storeValidationPort = storeValidationPort;
    this.documentTypeCandidatesPort = documentTypeCandidatesPort;
    this.processDocumentPort = processDocumentPort;
    this.processRepositoryPort = processRepositoryPort;
    this.processEventPort = processEventPort;
    this.defaultRepositoryId = defaultRepositoryId;
  }

  @Override
  public ValidationReport validate(
      String tenantId,
      String processId,
      String documentTypeId,
      List<CandidateAttribute> attributes,
      DvelopCredential credential) {
    Map<String, DmsPropertyDefinition> allowlist = allowlist(tenantId, processId, documentTypeId);
    Map<String, CandidateAttribute> sent = new LinkedHashMap<>();
    if (attributes != null) {
      for (CandidateAttribute attribute : attributes) {
        if (attribute != null && allowlist.containsKey(attribute.propertyId())) {
          sent.put(attribute.propertyId(), attribute);
        }
      }
    }
    String filename =
        processDocumentPort
            .find(tenantId, processId)
            .map(ProcessDocumentPort.StoredDocument::filename)
            .orElse(null);
    String repositoryId =
        processRepositoryPort.retrieve(tenantId, processId).orElse(defaultRepositoryId);

    StoreValidationResult result =
        storeValidationPort.validate(
            tenantId,
            new StoreValidationRequest(
                repositoryId,
                documentTypeId,
                filename,
                sent.values().stream()
                    .map(a -> new PropertyValue(a.propertyId(), a.value(), a.values()))
                    .toList(),
                allowlist),
            credential);
    return switch (result) {
      case StoreValidationResult.Invalid invalid -> ValidationReport.invalid(invalid.error());
      case StoreValidationResult.Valid valid ->
          ValidationReport.valid(adopt(valid, sent, allowlist));
    };
  }

  /**
   * Existence is checked through the tenant-filtered ports: another tenant's process has no
   * candidates and no steps, so it is a 404.
   */
  private Map<String, DmsPropertyDefinition> allowlist(
      String tenantId, String processId, String documentTypeId) {
    List<DmsDocumentMetadata> candidates =
        documentTypeCandidatesPort.find(tenantId, processId).orElse(List.of());
    for (DmsDocumentMetadata candidate : candidates) {
      if (candidate.documentType() != null
          && documentTypeId != null
          && documentTypeId.equals(candidate.documentType().id())) {
        Map<String, DmsPropertyDefinition> writable = new LinkedHashMap<>();
        candidate.documentType().properties().stream()
            .filter(DmsPropertyDefinition::isModifiable)
            .filter(property -> !property.isHidden())
            .filter(property -> !property.isSystemProperty())
            .forEach(property -> writable.putIfAbsent(property.id(), property));
        return writable;
      }
    }
    if (!processEventPort.exists(tenantId, processId)) {
      throw new ProcessNotFoundException(processId);
    }
    throw new InvalidRequestException("documentTypeId is not a document type of this process");
  }

  private static List<ValidatedAttribute> adopt(
      StoreValidationResult.Valid valid,
      Map<String, CandidateAttribute> sent,
      Map<String, DmsPropertyDefinition> allowlist) {
    Map<String, PropertyValue> returned = new LinkedHashMap<>();
    for (PropertyValue property : valid.returned()) {
      if (allowlist.containsKey(property.propertyId())) {
        returned.putIfAbsent(property.propertyId(), property);
      }
    }
    List<ValidatedAttribute> adopted = new ArrayList<>();
    for (PropertyValue submitted : valid.submitted()) {
      AttributeOrigin origin = originOf(sent.get(submitted.propertyId()));
      PropertyValue answer = returned.remove(submitted.propertyId());
      if (answer == null) {
        // Empty or absent in the answer: a suggestion is never deleted because the DMS is silent.
        adopted.add(attribute(submitted, origin, false));
      } else if (answer.asList().equals(submitted.asList())) {
        adopted.add(attribute(submitted, origin, false));
      } else {
        adopted.add(attribute(answer, origin, true));
      }
    }
    // Not sent (empty or unknown) but set by the DMS: hook or default.
    returned.values().forEach(answer -> adopted.add(attribute(answer, AttributeOrigin.DMS, true)));
    return adopted;
  }

  private static AttributeOrigin originOf(CandidateAttribute attribute) {
    return attribute == null || attribute.origin() == null
        ? AttributeOrigin.LLM
        : attribute.origin();
  }

  private static ValidatedAttribute attribute(
      PropertyValue property, AttributeOrigin origin, boolean changedByDms) {
    return new ValidatedAttribute(
        property.propertyId(), property.value(), property.values(), origin, changedByDms);
  }
}
