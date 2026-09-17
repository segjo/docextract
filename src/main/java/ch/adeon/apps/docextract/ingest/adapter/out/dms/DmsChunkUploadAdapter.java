package ch.adeon.apps.docextract.ingest.adapter.out.dms;

import ch.adeon.apps.docextract.ingest.application.DmsChunkUploadException;
import ch.adeon.apps.docextract.ingest.application.DmsChunkUploadPort;
import ch.adeon.apps.docextract.ingest.domain.DmsLocation;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.security.application.AppConfigPort;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Uploads document- and preview-chunks to the write-only d.velop DMS-Chunk-Store via {@code POST
 * /r/{repositoryId}/blob/chunk} (operationId {@code uploadBlobChunk}). Only the returned {@code
 * Location} header is kept as the later finalization target; the bytes are never read back from
 * here (ADR-008) — the transient Postgres blobstore (see {@code out.blob}) serves that purpose.
 */
@Component
public class DmsChunkUploadAdapter implements DmsChunkUploadPort {

  private final RestClient restClient;
  private final AppConfigPort appConfigPort;
  private final String repositoryId;
  private final String origin;

  public DmsChunkUploadAdapter(
      AppConfigPort appConfigPort,
      @Value("${docextract.ingest.dms.repository-id}") String repositoryId,
      @Value("${docextract.ingest.dms.origin}") String origin) {
    this.restClient = RestClient.create();
    this.appConfigPort = appConfigPort;
    this.repositoryId = repositoryId;
    this.origin = origin;
  }

  @Override
  public DmsLocation upload(byte[] content, MediaType mediaType, DvelopCredential credential) {
    try {
      ResponseEntity<Void> response =
          restClient
              .post()
              .uri(appConfigPort.systemBaseUri() + "/dms/r/{repositoryId}/blob/chunk", repositoryId)
              // Origin is mandatory for write access (POST/PUT/DELETE/PATCH) to prevent CSRF.
              .header(HttpHeaders.ORIGIN, origin)
              .header(credential.headerName(), credential.headerValue())
              .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
              .body(content)
              .retrieve()
              .toBodilessEntity();
      String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
      if (location == null || location.isBlank()) {
        throw new DmsChunkUploadException(
            "d.velop DMS did not return a Location header for the uploaded chunk");
      }
      return new DmsLocation(location);
    } catch (RestClientException ex) {
      throw new DmsChunkUploadException(
          "Uploading the document chunk to the d.velop DMS failed", ex);
    }
  }
}
