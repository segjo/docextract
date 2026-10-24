import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { ExtractionResult } from '../models/extraction.model';

@Service()
export class ExtractionApi {
  private readonly http = inject(HttpClient);

  extractionResult(processId: string): Observable<ExtractionResult> {
    return this.http.get<ExtractionResult>(
      `${API_BASE}/processes/${encodeURIComponent(processId)}/extraction-result`,
    );
  }

  /** Re-runs extraction forcing the document type to the reviewer-chosen `documentTypeId`. */
  retryExtraction(processId: string, documentTypeId: string): Observable<ExtractionResult> {
    return this.http.post<ExtractionResult>(
      `${API_BASE}/processes/${encodeURIComponent(processId)}/extraction-result/retry`,
      { documentTypeId },
    );
  }
}
