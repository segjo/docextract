package ch.adeon.apps.docextract.retrieval.domain;

import java.util.List;

/**
 * A DMS object's document type: {@code id} is the technical category code (the {@code
 * property_category} property's raw value, e.g. {@code "DREC"}), {@code name} its display name
 * (e.g. {@code "Rechnung"}). {@code id} is {@code null} if the response carried no {@code
 * property_category} entry to read it from. {@code properties} holds every property definition
 * possible for this type; a {@link DmsDocumentMetadata}'s own {@code properties} only reference
 * these by {@code id} and carry the actual value.
 */
public record DmsDocumentType(String id, String name, List<DmsPropertyDefinition> properties) {}
