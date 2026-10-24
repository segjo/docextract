import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { ImportInfo, IngestedDocument } from '../models/ingest.model';
import { MOCK_API } from '../mock/mock-flag';

@Service()
export class IngestApi {
  private readonly http = inject(HttpClient);

  /** Called once when the import mask opens (accepted file extensions, max size, repositories). */
  importInfo(): Observable<ImportInfo> {
    return this.http.get<ImportInfo>(`${API_BASE}/ingest/import-info`);
  }

  upload(file: File, repositoryId: string | null): Observable<IngestedDocument> {
    const body = new FormData();
    body.append('file', file);
    if (repositoryId) {
      body.append('repositoryId', repositoryId);
    }
    return this.http.post<IngestedDocument>(`${API_BASE}/ingest/documents`, body);
  }

  /**
   * Streamed via `<object data>`/`<iframe>`, not fetched through HttpClient (byte-range PDF
   * preview) — bypasses `mockApiInterceptor` entirely, so mock mode points at a static asset.
   */
  previewUrl(blobId: string): string {
    if (MOCK_API) {
      return '/mock/preview.html';
    }
    return `${API_BASE}/ingest/documents/${encodeURIComponent(blobId)}/preview`;
  }
}
