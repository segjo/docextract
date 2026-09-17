package ch.adeon.apps.docextract.ingest.adapter.out.blob;

import ch.adeon.apps.docextract.ingest.application.BlobNotFoundException;
import ch.adeon.apps.docextract.ingest.application.DocumentBlobPort;
import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transient blobstore for Roh-/Preview-Bytes (ADR-008): header data lives in {@code document_blob},
 * content in 1-MiB {@code document_blob_page} segments so reads stay range-capable without
 * materializing the whole object. TTL-Cleanup and the actual ACL pre-filter enforcement (NfA-4)
 * live in the application layer; this adapter only persists what it is given.
 */
@Component
public class PgDocumentBlobAdapter implements DocumentBlobPort {

  static final int PAGE_SIZE_BYTES = 1024 * 1024;
  static final long MAX_BLOB_SIZE_BYTES = 50L * 1024 * 1024;

  private final JdbcTemplate jdbcTemplate;
  private final Duration blobTtl;

  public PgDocumentBlobAdapter(
      JdbcTemplate jdbcTemplate, @Value("${docextract.ingest.blob-ttl:PT2H}") Duration blobTtl) {
    this.jdbcTemplate = jdbcTemplate;
    this.blobTtl = blobTtl;
  }

  @Override
  @Transactional
  public BlobRef store(
      BlobKind kind,
      MediaType mediaType,
      byte[] content,
      String processId,
      String tenantId,
      String userId) {
    if (content.length > MAX_BLOB_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "blob of %d bytes exceeds the %d byte hard cap (ADR-008)"
              .formatted(content.length, MAX_BLOB_SIZE_BYTES));
    }

    UUID blobId = UUID.randomUUID();
    Timestamp expiresAt = Timestamp.from(Instant.now().plus(blobTtl));
    jdbcTemplate.update(
        """
        INSERT INTO document_blob
            (blob_id, kind, media_type, size_bytes, process_id, tenant_id, user_id, expires_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        blobId,
        kind.name(),
        mediaType.value(),
        (long) content.length,
        processId,
        tenantId,
        userId,
        expiresAt);

    int segmentNo = 0;
    for (int offset = 0; offset < content.length; offset += PAGE_SIZE_BYTES) {
      int end = Math.min(offset + PAGE_SIZE_BYTES, content.length);
      jdbcTemplate.update(
          "INSERT INTO document_blob_page (blob_id, segment_no, bytes) VALUES (?, ?, ?)",
          blobId,
          segmentNo,
          Arrays.copyOfRange(content, offset, end));
      segmentNo++;
    }

    return describe(blobId);
  }

  @Override
  public BlobRef describe(UUID blobId) {
    List<BlobRef> rows =
        jdbcTemplate.query(
            """
            SELECT blob_id, kind, media_type, size_bytes, process_id, tenant_id, user_id
            FROM document_blob
            WHERE blob_id = ?
            """,
            (rs, rowNum) ->
                new BlobRef(
                    UUID.fromString(rs.getString("blob_id")),
                    BlobKind.valueOf(rs.getString("kind")),
                    MediaType.of(rs.getString("media_type")),
                    rs.getLong("size_bytes"),
                    rs.getString("process_id"),
                    rs.getString("tenant_id"),
                    rs.getString("user_id")),
            blobId);
    return rows.stream().findFirst().orElseThrow(() -> new BlobNotFoundException(blobId));
  }

  @Override
  public byte[] readRange(UUID blobId, PageRange range) {
    BlobRef blob = describe(blobId);
    long clampedEnd = Math.min(range.endInclusive(), blob.sizeBytes() - 1);
    if (clampedEnd < range.start()) {
      return new byte[0];
    }

    int firstSegment = (int) (range.start() / PAGE_SIZE_BYTES);
    int lastSegment = (int) (clampedEnd / PAGE_SIZE_BYTES);

    List<byte[]> pages =
        jdbcTemplate.query(
            """
            SELECT bytes FROM document_blob_page
            WHERE blob_id = ? AND segment_no BETWEEN ? AND ?
            ORDER BY segment_no
            """,
            (rs, rowNum) -> rs.getBytes("bytes"),
            blobId,
            firstSegment,
            lastSegment);

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    pages.forEach(buffer::writeBytes);
    byte[] concatenated = buffer.toByteArray();

    long segmentBaseOffset = (long) firstSegment * PAGE_SIZE_BYTES;
    int sliceStart = (int) (range.start() - segmentBaseOffset);
    int sliceEnd = Math.min((int) (clampedEnd - segmentBaseOffset) + 1, concatenated.length);
    return Arrays.copyOfRange(concatenated, sliceStart, Math.max(sliceStart, sliceEnd));
  }

  @Override
  public void delete(UUID blobId) {
    jdbcTemplate.update("DELETE FROM document_blob WHERE blob_id = ?", blobId);
  }
}
