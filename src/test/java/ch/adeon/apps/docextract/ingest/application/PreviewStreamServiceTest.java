package ch.adeon.apps.docextract.ingest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import ch.adeon.apps.docextract.security.application.AuthContextPort;
import ch.adeon.apps.docextract.security.domain.AuthContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreviewStreamServiceTest {

  private static final AuthContext OWNER = new AuthContext("tenant-a", "acl", "user-1", "User One");

  private final DocumentBlobPort documentBlobPort = mock(DocumentBlobPort.class);
  private final PreviewRenderPort previewRenderPort = mock(PreviewRenderPort.class);
  private final AuthContextPort authContextPort = mock(AuthContextPort.class);
  private final PreviewStreamService service =
      new PreviewStreamService(documentBlobPort, previewRenderPort, authContextPort);

  @Test
  void pdf_is_streamed_directly_from_the_original_blob_without_rendering() {
    UUID originalBlobId = UUID.randomUUID();
    when(authContextPort.current()).thenReturn(OWNER);
    when(documentBlobPort.describe(originalBlobId))
        .thenReturn(
            new BlobRef(
                originalBlobId, BlobKind.ORIGINAL, MediaType.PDF, 4, "p1", "tenant-a", "user-1"));
    when(documentBlobPort.readRange(eq(originalBlobId), any())).thenReturn(new byte[] {1, 2, 3, 4});

    PreviewChunk result = service.stream(originalBlobId, null);

    assertThat(result.content()).containsExactly(1, 2, 3, 4);
    verify(previewRenderPort, never()).renderPreview(any(), any(), any());
  }

  @Test
  void non_pdf_is_rendered_once_and_streamed_from_the_preview_blob() {
    UUID originalBlobId = UUID.randomUUID();
    UUID previewBlobId = UUID.randomUUID();
    MediaType docx =
        MediaType.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    when(authContextPort.current()).thenReturn(OWNER);
    when(documentBlobPort.describe(originalBlobId))
        .thenReturn(
            new BlobRef(originalBlobId, BlobKind.ORIGINAL, docx, 3, "p1", "tenant-a", "user-1"));
    when(documentBlobPort.readRange(eq(originalBlobId), any())).thenReturn(new byte[] {1, 2, 3});
    when(previewRenderPort.renderPreview(anyString(), eq(docx), any())).thenReturn(new byte[] {9});
    when(documentBlobPort.store(
            eq(BlobKind.PREVIEW), eq(MediaType.PDF), any(), eq("p1"), eq("tenant-a"), eq("user-1")))
        .thenReturn(
            new BlobRef(
                previewBlobId, BlobKind.PREVIEW, MediaType.PDF, 1, "p1", "tenant-a", "user-1"));
    when(documentBlobPort.readRange(eq(previewBlobId), any())).thenReturn(new byte[] {9});

    PreviewChunk result = service.stream(originalBlobId, null);

    assertThat(result.content()).containsExactly(9);
  }

  @Test
  void a_given_range_is_passed_through_to_the_blobstore() {
    UUID originalBlobId = UUID.randomUUID();
    when(authContextPort.current()).thenReturn(OWNER);
    when(documentBlobPort.describe(originalBlobId))
        .thenReturn(
            new BlobRef(
                originalBlobId, BlobKind.ORIGINAL, MediaType.PDF, 10, "p1", "tenant-a", "user-1"));
    PageRange range = new PageRange(2, 4);
    when(documentBlobPort.readRange(originalBlobId, range)).thenReturn(new byte[] {5, 6, 7});

    PreviewChunk result = service.stream(originalBlobId, range);

    assertThat(result.content()).containsExactly(5, 6, 7);
    assertThat(result.totalSize()).isEqualTo(10);
  }

  @Test
  void an_open_ended_range_is_clamped_to_the_actual_blob_size_without_overflowing() {
    UUID originalBlobId = UUID.randomUUID();
    when(authContextPort.current()).thenReturn(OWNER);
    when(documentBlobPort.describe(originalBlobId))
        .thenReturn(
            new BlobRef(
                originalBlobId, BlobKind.ORIGINAL, MediaType.PDF, 10, "p1", "tenant-a", "user-1"));
    PageRange openEnded = new PageRange(7, Long.MAX_VALUE);
    when(documentBlobPort.readRange(originalBlobId, openEnded)).thenReturn(new byte[] {8, 9, 10});

    PreviewChunk result = service.stream(originalBlobId, openEnded);

    assertThat(result.content()).containsExactly(8, 9, 10);
    assertThat(result.totalSize()).isEqualTo(10);
  }

  @Test
  void a_different_tenant_is_denied_as_if_the_blob_did_not_exist() {
    UUID originalBlobId = UUID.randomUUID();
    when(authContextPort.current())
        .thenReturn(new AuthContext("tenant-b", "acl", "user-1", "User One"));
    when(documentBlobPort.describe(originalBlobId))
        .thenReturn(
            new BlobRef(
                originalBlobId, BlobKind.ORIGINAL, MediaType.PDF, 4, "p1", "tenant-a", "user-1"));

    assertThatThrownBy(() -> service.stream(originalBlobId, null))
        .isInstanceOf(BlobNotFoundException.class);
  }

  @Test
  void a_different_user_of_the_same_tenant_is_denied_as_if_the_blob_did_not_exist() {
    UUID originalBlobId = UUID.randomUUID();
    when(authContextPort.current())
        .thenReturn(new AuthContext("tenant-a", "acl", "user-2", "User Two"));
    when(documentBlobPort.describe(originalBlobId))
        .thenReturn(
            new BlobRef(
                originalBlobId, BlobKind.ORIGINAL, MediaType.PDF, 4, "p1", "tenant-a", "user-1"));

    assertThatThrownBy(() -> service.stream(originalBlobId, null))
        .isInstanceOf(BlobNotFoundException.class);
  }
}
