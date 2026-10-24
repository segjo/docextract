import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import { ConfirmationResult, ConfirmRequest, RejectRequest } from '../models/validation.model';

/** Human-in-the-Loop gate (C-2) — the only path that can trigger a DMS write. */
@Service()
export class ValidationApi {
  private readonly http = inject(HttpClient);

  confirm(request: ConfirmRequest): Observable<ConfirmationResult> {
    return this.http.post<ConfirmationResult>(`${API_BASE}/validation/confirm`, request);
  }

  reject(request: RejectRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE}/validation/reject`, request);
  }
}
