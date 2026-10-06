package ch.adeon.apps.docextract.retrieval.port;

import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.List;
import java.util.Map;

/**
 * Outbound port: live lookup of a single property's valid values via the d.velop valuelist webhook
 * ({@code POST /dms/r/{repositoryId}/validvalues/p/{propertyId}}, undocumented in
 * dvelop-dmsapp.yaml — payload/response shape only verified against a captured live example, SPEC
 * §3). {@code extendedProperties}/{@code multivalueExtendedProperties} carry the document's own
 * currently known property values as filter context — some value lists depend on another property
 * and return no values until that one is filled in (caller is expected to retry later with more
 * context, e.g. from an LLM's extraction suggestion). {@code searchTerm}, if given, additionally
 * narrows the result to values starting with it (verified live: the webhook filters when {@code
 * extendedProperties} carries the looked-up property's own id mapped to the search term) — useful
 * for a "load more"/typeahead UI without raising {@code maxValues}. Capped server-side to {@code
 * maxValues} to bound response size (T-4); {@link
 * ch.adeon.apps.docextract.retrieval.domain.DmsValueList#hasMore()} tells the caller whether more
 * values existed than the cap allowed through.
 */
public interface ValueListPort {

  DmsValueList fetchValues(
      String repositoryId,
      String objectDefinitionId,
      String propertyId,
      Map<String, String> extendedProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String searchTerm,
      int maxValues,
      DvelopCredential credential);
}
