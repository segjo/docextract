package ch.adeon.apps.docextract.retrieval.domain;

import java.util.List;

/**
 * Indexing-relevant view of a DMS object's properties (FR-3, SPEC §3): the document's document type
 * (id, name and every possible property's definition, see {@link DmsDocumentType}), this document's
 * actual property values (see {@link DmsProperty}, referencing the type's property definitions by
 * id) and the response's content language. Deliberately leaves out everything else in the raw
 * d.velop response (blob/preview metadata, HAL links, ...) — this is meant to be compact context
 * for an LLM, not the full response.
 */
public record DmsDocumentMetadata(
    DmsDocumentType documentType, String contentLanguage, List<DmsProperty> properties) {}
