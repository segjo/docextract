/** Pipeline steps reported over SSE for a processId — mirrors the backend's ProcessStep enum. */
export type ProcessStep =
  'BLOB_STORED' | 'DMS_UPLOADED' | 'PREVIEW' | 'TEXT_EXTRACTED' | 'RETRIEVED' | 'EXTRACTED';

/** Order in which pipeline steps are shown in the protocol panel (docextract_ascii_layouts.md). */
export const PROCESS_STEP_ORDER: ProcessStep[] = [
  'DMS_UPLOADED',
  'PREVIEW',
  'TEXT_EXTRACTED',
  'RETRIEVED',
  'EXTRACTED',
];

/** German labels from the Layout-2 wireframe, in `PROCESS_STEP_ORDER`. */
export const PROCESS_STEP_LABELS: Record<ProcessStep, string> = {
  BLOB_STORED: 'Datei gespeichert',
  DMS_UPLOADED: 'Upload & Eingangspruefung',
  PREVIEW: 'Vorschau generieren',
  TEXT_EXTRACTED: 'Dokument strukturieren',
  RETRIEVED: 'Aehnliche Dokumente suchen',
  EXTRACTED: 'Eigenschaften extrahieren',
};

export type StepStatus = 'STARTED' | 'COMPLETED' | 'FAILED';

export interface ProcessEvent {
  processId: string;
  step: ProcessStep;
  status: StepStatus;
  blobId: string | null;
}
