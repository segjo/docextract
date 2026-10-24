import { ProcessEvent } from './models/process.model';
import {
  deriveStepStatuses,
  findPreviewBlobId,
  hasFailure,
  isExtractionComplete,
} from './process-progress';

function event(partial: Partial<ProcessEvent>): ProcessEvent {
  return { processId: 'p1', step: 'DMS_UPLOADED', status: 'COMPLETED', blobId: null, ...partial };
}

describe('deriveStepStatuses', () => {
  it('marks every known step as pending when there are no events', () => {
    const statuses = deriveStepStatuses([]);
    expect(statuses['DMS_UPLOADED']).toBe('pending');
    expect(statuses['EXTRACTED']).toBe('pending');
  });

  it('maps STARTED/COMPLETED/FAILED to running/done/error', () => {
    const statuses = deriveStepStatuses([
      event({ step: 'DMS_UPLOADED', status: 'COMPLETED' }),
      event({ step: 'PREVIEW', status: 'STARTED' }),
      event({ step: 'TEXT_EXTRACTED', status: 'FAILED' }),
    ]);
    expect(statuses['DMS_UPLOADED']).toBe('done');
    expect(statuses['PREVIEW']).toBe('running');
    expect(statuses['TEXT_EXTRACTED']).toBe('error');
    expect(statuses['RETRIEVED']).toBe('pending');
  });

  it('keeps the latest status when a step is reported more than once', () => {
    const statuses = deriveStepStatuses([
      event({ step: 'PREVIEW', status: 'STARTED' }),
      event({ step: 'PREVIEW', status: 'COMPLETED' }),
    ]);
    expect(statuses['PREVIEW']).toBe('done');
  });

  it('ignores steps not shown in the protocol panel (e.g. BLOB_STORED)', () => {
    const statuses = deriveStepStatuses([event({ step: 'BLOB_STORED', status: 'COMPLETED' })]);
    expect(statuses['DMS_UPLOADED']).toBe('pending');
  });
});

describe('findPreviewBlobId', () => {
  it('returns null when no PREVIEW step has completed', () => {
    expect(findPreviewBlobId([event({ step: 'PREVIEW', status: 'STARTED' })])).toBeNull();
  });

  it('returns the blobId of the completed PREVIEW event', () => {
    const result = findPreviewBlobId([
      event({ step: 'PREVIEW', status: 'COMPLETED', blobId: 'blob-1' }),
    ]);
    expect(result).toBe('blob-1');
  });

  it('returns the most recent completed PREVIEW event', () => {
    const result = findPreviewBlobId([
      event({ step: 'PREVIEW', status: 'COMPLETED', blobId: 'blob-1' }),
      event({ step: 'PREVIEW', status: 'COMPLETED', blobId: 'blob-2' }),
    ]);
    expect(result).toBe('blob-2');
  });
});

describe('hasFailure', () => {
  it('is false when no event failed', () => {
    expect(hasFailure([event({ status: 'COMPLETED' })])).toBe(false);
  });

  it('is true when any event failed', () => {
    expect(hasFailure([event({ status: 'COMPLETED' }), event({ status: 'FAILED' })])).toBe(true);
  });
});

describe('isExtractionComplete', () => {
  it('is false without a completed EXTRACTED event', () => {
    expect(isExtractionComplete([event({ step: 'EXTRACTED', status: 'STARTED' })])).toBe(false);
  });

  it('is true once EXTRACTED has completed', () => {
    expect(isExtractionComplete([event({ step: 'EXTRACTED', status: 'COMPLETED' })])).toBe(true);
  });
});
