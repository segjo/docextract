package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.port.DmsObjectDefinitionPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GetObjectDefinitionsService implements GetObjectDefinitions {

  private final DmsObjectDefinitionPort dmsObjectDefinitionPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final String repositoryId;

  public GetObjectDefinitionsService(
      DmsObjectDefinitionPort dmsObjectDefinitionPort,
      OutboundCredentialPort outboundCredentialPort,
      @Value("${docextract.ingest.dms.repository-id}") String repositoryId) {
    this.dmsObjectDefinitionPort = dmsObjectDefinitionPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.repositoryId = repositoryId;
  }

  @Override
  public List<DmsDocumentMetadata> get() {
    return dmsObjectDefinitionPort.fetchAll(repositoryId, outboundCredentialPort.current());
  }
}
