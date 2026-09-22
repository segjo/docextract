package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import java.util.List;
import java.util.Map;

/**
 * Inbound port: live lookup of a single property's valid values (SPEC §3), for the frontend to call
 * directly — both for the initial (unfiltered) list and for typeahead/"load more" filtering via
 * {@code searchTerm} (see {@link ValueListPort}).
 */
public interface GetValueList {

  DmsValueList get(
      String objectDefinitionId,
      String propertyId,
      Map<String, String> extendedProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String searchTerm);
}
