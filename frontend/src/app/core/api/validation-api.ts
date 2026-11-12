import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from './api-base';
import {
  CheckRequest,
  ConfirmationResult,
  ConfirmRequest,
  RejectRequest,
  ValidationReport,
} from '../models/validation.model';

/** Human-in-the-Loop gate (C-2) — the only path that can trigger a DMS write. */
@Service()
export class ValidationApi {
  private readonly http = inject(HttpClient);

  /** Read-only DMS check of the current values (ADR-016); never writes, never blocks `confirm`. */
  check(request: CheckRequest): Observable<ValidationReport> {
    return this.http.post<ValidationReport>(`${API_BASE}/validation/check`, request);
  }

  confirm(request: ConfirmRequest): Observable<ConfirmationResult> {
    return this.http.post<ConfirmationResult>(`${API_BASE}/validation/confirm`, request);
  }

  reject(request: RejectRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE}/validation/reject`, request);
  }
}
