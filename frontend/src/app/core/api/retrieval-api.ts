import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import {
  DmsDocumentMetadata,
  DmsValueList,
  RetrievalResult,
  ValueListRequest,
} from '../models/retrieval.model';

@Service()
export class RetrievalApi {
  private readonly http = inject(HttpClient);

  similarDocuments(processId: string): Observable<RetrievalResult[]> {
    return this.http.get<RetrievalResult[]>(
      `${API_BASE}/processes/${encodeURIComponent(processId)}/similar-documents`,
    );
  }

  documentTypeCandidates(processId: string): Observable<DmsDocumentMetadata[]> {
    return this.http.get<DmsDocumentMetadata[]>(
      `${API_BASE}/processes/${encodeURIComponent(processId)}/document-type-candidates`,
    );
  }

  valueList(propertyId: string, request: ValueListRequest): Observable<DmsValueList> {
    return this.http.post<DmsValueList>(
      `${API_BASE}/value-lists/${encodeURIComponent(propertyId)}`,
      request,
    );
  }
}
