import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { IngestApi } from '../../../core/api/ingest-api';
import { DocumentViewer } from './document-viewer';

describe('DocumentViewer', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({
      imports: [DocumentViewer],
      providers: [provideHttpClient()],
    }).compileComponents();
    return TestBed.createComponent(DocumentViewer);
  }

  it('shows the placeholder while no preview blobId is available', async () => {
    const fixture = await createComponent();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.viewer__placeholder')).toBeTruthy();
    expect(el.querySelector('iframe')).toBeNull();
  });

  it('sets the iframe src from IngestApi.previewUrl once a blobId is available', async () => {
    const fixture = await createComponent();
    const ingestApi = TestBed.inject(IngestApi);
    vi.spyOn(ingestApi, 'previewUrl').mockReturnValue('/api/v1/ingest/documents/blob-1/preview');

    fixture.componentRef.setInput('previewBlobId', 'blob-1');
    fixture.detectChanges();

    const iframe = (fixture.nativeElement as HTMLElement).querySelector(
      'iframe',
    ) as HTMLIFrameElement;
    expect(iframe).toBeTruthy();
    expect(ingestApi.previewUrl).toHaveBeenCalledWith('blob-1');
    expect(iframe.src).toContain('/api/v1/ingest/documents/blob-1/preview');
  });
});
