package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.security.application.DvelopCredential;
import java.util.List;

/**
 * The only way to the DMS check {@code storevalidate} (ADR-016): read-only, no local mandatory
 * field check (the DMS hooks may set them). Shared by the REST endpoint and the LLM tool.
 */
public interface ValidateAttributes {

  /**
   * @throws ch.adeon.apps.docextract.process.application.ProcessNotFoundException unknown or
   *     foreign process
   * @throws ch.adeon.apps.docextract.shared.error.InvalidRequestException {@code documentTypeId} is
   *     no staged candidate of the process
   * @throws ch.adeon.apps.docextract.shared.error.AdapterUnavailableException the DMS cannot answer
   */
  ValidationReport validate(
      String tenantId,
      String processId,
      String documentTypeId,
      List<CandidateAttribute> attributes,
      DvelopCredential credential);
}
