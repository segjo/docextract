import { HttpEvent, HttpHandlerFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
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
    body: (req) => mockRetryExtractionResult((req.body as { documentTypeId: string }).documentTypeId),
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
  { method: 'GET', pattern: /^\/object-definitions$/, body: mockDocumentTypeCandidates },
  { method: 'POST', pattern: /^\/value-lists\/[^/]+$/, body: mockValueList },
  { method: 'POST', pattern: /^\/validation\/confirm$/, body: mockConfirmationResult },
  { method: 'POST', pattern: /^\/validation\/reject$/, body: () => undefined },
];

/** Intercepts all `API_BASE` calls with canned data — active only in the `mock` build configuration. */
export function mockApiInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  if (!MOCK_API || !req.url.startsWith(API_BASE)) {
    return next(req);
  }
  const path = req.url.slice(API_BASE.length);
  const route = MOCK_ROUTES.find((r) => r.method === req.method && r.pattern.test(path));
  return route ? mockResponse(route.body(req)) : next(req);
}
