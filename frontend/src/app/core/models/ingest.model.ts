/** Response of `POST /api/v1/ingest/documents` — upload accepted, async pipeline started (202-style flow). */
export interface IngestedDocument {
  processId: string;
  dmsLocation: string | null;
  originalBlobId: string;
  mediaType: string;
}

/** A d.velop repository connection the current user can import into (`GET /repo/repositories`). */
export interface DmsRepository {
  id: string;
  name: string;
  isDefault: boolean;
  isAvailable: boolean;
  isReachable: boolean;
}

/**
 * Response of `GET /api/v1/ingest/import-info` — fetched once when the import mask opens so the
 * frontend never hardcodes the accepted file extensions, the max upload size, or the repository
 * list.
 */
export interface ImportInfo {
  acceptedFileExtensions: string[];
  maxFileSizeBytes: number;
  repositories: DmsRepository[];
}
