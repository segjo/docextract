package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.DmsRepository;
import ch.adeon.apps.docextract.ingest.domain.ImportInfo;
import ch.adeon.apps.docextract.ingest.port.DmsRepositoryPort;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GetImportInfoService implements GetImportInfo {

  private final DmsRepositoryPort dmsRepositoryPort;
  private final OutboundCredentialPort outboundCredentialPort;
  private final GeneratePreview generatePreview;
  private final long maxFileSizeBytes;

  public GetImportInfoService(
      DmsRepositoryPort dmsRepositoryPort,
      OutboundCredentialPort outboundCredentialPort,
      GeneratePreview generatePreview,
      @Value("${docextract.ingest.max-file-size-bytes:26214400}") long maxFileSizeBytes) {
    this.dmsRepositoryPort = dmsRepositoryPort;
    this.outboundCredentialPort = outboundCredentialPort;
    this.generatePreview = generatePreview;
    this.maxFileSizeBytes = maxFileSizeBytes;
  }

  @Override
  public ImportInfo get() {
    List<String> acceptedFileExtensions =
        generatePreview.supportedFileExtensions().stream().sorted().toList();
    List<DmsRepository> repositories = dmsRepositoryPort.fetchAll(outboundCredentialPort.current());
    return new ImportInfo(acceptedFileExtensions, maxFileSizeBytes, repositories);
  }
}
