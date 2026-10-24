import { TestBed } from '@angular/core/testing';
import { ProcessEvent } from '../../../core/models/process.model';
import { ProtocolPanel } from './protocol-panel';

describe('ProtocolPanel', () => {
  async function createComponent(events: ProcessEvent[]) {
    await TestBed.configureTestingModule({ imports: [ProtocolPanel] }).compileComponents();
    const fixture = TestBed.createComponent(ProtocolPanel);
    fixture.componentRef.setInput('processId', 'p-123');
    fixture.componentRef.setInput('events', events);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the processId and one entry per pipeline step', async () => {
    const fixture = await createComponent([]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.protocol__process-id')!.textContent).toContain('p-123');
    expect(el.querySelectorAll('.protocol__step').length).toBe(5);
  });

  it('shows the running/done status for reported steps', async () => {
    const fixture = await createComponent([
      { processId: 'p-123', step: 'DMS_UPLOADED', status: 'COMPLETED', blobId: null },
      { processId: 'p-123', step: 'PREVIEW', status: 'STARTED', blobId: null },
    ]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.protocol__step--done')).toBeTruthy();
    expect(el.querySelector('.protocol__step--running')).toBeTruthy();
    expect(el.querySelectorAll('.protocol__step--pending').length).toBe(3);
  });

  it('shows the error status once a step fails', async () => {
    const fixture = await createComponent([
      { processId: 'p-123', step: 'TEXT_EXTRACTED', status: 'FAILED', blobId: null },
    ]);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.protocol__step--error'),
    ).toBeTruthy();
  });
});
