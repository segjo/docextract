import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { isDevMode } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, delay } from 'rxjs/operators';
import { API_BASE } from '../api/api-base';
import { MOCK_API } from './mock-flag';
import {
  mockConfirmationResult,
  mockDocumentTypeCandidates,
  mockExtractionResult,
  mockImportInfo,
  mockIngestedDocument,
  mockRetryExtractionResult,
  mockSimilarDocuments,
  mockValueList,
} from './mock-data';

const MOCK_LATENCY_MS = 300;

function mockResponse<T>(body: T): Observable<HttpResponse<T>> {
  return of(new HttpResponse({ status: 200, body })).pipe(delay(MOCK_LATENCY_MS));
}

interface MockRoute {
  method: string;
  pattern: RegExp;
  body: (req: HttpRequest<unknown>) => unknown;
}

const MOCK_ROUTES: MockRoute[] = [
  { method: 'GET', pattern: /^\/ingest\/import-info$/, body: mockImportInfo },
  { method: 'POST', pattern: /^\/ingest\/documents$/, body: mockIngestedDocument },
  {
    method: 'GET',
    pattern: /^\/processes\/[^/]+\/extraction-result$/,
    body: mockExtractionResult,
  },
  {
    method: 'POST',
    pattern: /^\/processes\/[^/]+\/extraction-result\/retry$/,
    body: (req) =>
      mockRetryExtractionResult((req.body as { documentTypeId: string }).documentTypeId),
  },
  {
    method: 'GET',
    pattern: /^\/processes\/[^/]+\/similar-documents$/,
    body: mockSimilarDocuments,
  },
  {
    method: 'GET',
    pattern: /^\/processes\/[^/]+\/document-type-candidates$/,
    body: mockDocumentTypeCandidates,
  },
  { method: 'POST', pattern: /^\/value-lists\/[^/]+$/, body: mockValueList },
  { method: 'POST', pattern: /^\/validation\/confirm$/, body: mockConfirmationResult },
  { method: 'POST', pattern: /^\/validation\/reject$/, body: () => undefined },
];

/**
 * Intercepts all `API_BASE` calls with canned data in the `mock` build configuration. In dev mode
 * without it, canned data is only used as a fallback when the backend is unreachable or failing.
 */
export function mockApiInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  if (!req.url.startsWith(API_BASE) || (!MOCK_API && !isDevMode())) {
    return next(req);
  }
  const path = req.url.slice(API_BASE.length);
  const route = MOCK_ROUTES.find((r) => r.method === req.method && r.pattern.test(path));
  if (!route) {
    return next(req);
  }
  if (MOCK_API) {
    return mockResponse(route.body(req));
  }
  return next(req).pipe(
    catchError((error: unknown) =>
      error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500)
        ? mockResponse(route.body(req))
        : throwError(() => error),
    ),
  );
}
