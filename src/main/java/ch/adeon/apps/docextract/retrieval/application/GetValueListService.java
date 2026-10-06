package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import ch.adeon.apps.docextract.retrieval.port.ValueListPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GetValueListService implements GetValueList {

  private final ValueListPort valueListPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final String repositoryId;
  private final int maxValueListValues;

  public GetValueListService(
      ValueListPort valueListPort,
      OutboundCredentialPort outboundCredentialPort,
      @Value("${docextract.ingest.dms.repository-id}") String repositoryId,
      @Value("${docextract.valuelist.max-values:50}") int maxValueListValues) {
    this.valueListPort = valueListPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.repositoryId = repositoryId;
    this.maxValueListValues = maxValueListValues;
  }

  @Override
  public DmsValueList get(
      String objectDefinitionId,
      String propertyId,
      Map<String, String> extendedProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String searchTerm) {
    return valueListPort.fetchValues(
        repositoryId,
        objectDefinitionId,
        propertyId,
        extendedProperties == null ? Map.of() : extendedProperties,
        multivalueExtendedProperties == null ? Map.of() : multivalueExtendedProperties,
        searchTerm,
        maxValueListValues,
        outboundCredentialPort.current());
  }
}
