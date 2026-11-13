package ch.adeon.apps.docextract.validation.port;

import ch.adeon.apps.docextract.retrieval.port.DmsPropertyDefinition;
import java.util.List;
import java.util.Map;

/**
 * Input of the read-only DMS check (ADR-016). {@code definitions} is the allowlist: the writable
 * non-system properties of the object definition, by property id; they give the adapter the data
 * type and multiplicity of each value. {@code filename} may be {@code null}.
 */
public record StoreValidationRequest(
    String repositoryId,
    String objectDefinitionId,
    String filename,
    List<PropertyValue> properties,
    Map<String, DmsPropertyDefinition> definitions) {}
