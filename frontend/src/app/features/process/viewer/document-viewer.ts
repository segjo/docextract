import { Component, ElementRef, effect, inject, input, viewChild } from '@angular/core';
import { IngestApi } from '../../../core/api/ingest-api';

@Component({
  selector: 'app-document-viewer',
  templateUrl: './document-viewer.html',
  styleUrl: './document-viewer.scss',
})
export class DocumentViewer {
  private readonly ingestApi = inject(IngestApi);

  readonly previewBlobId = input<string | null>(null);

  private readonly frame = viewChild<ElementRef<HTMLIFrameElement>>('frame');

  constructor() {
    // Imperative DOM assignment (not an Angular template binding), so the same-origin, backend-
    // controlled preview URL never has to pass through the sanitizer's resource-url context — no
    // trust-bypass API needed (NfA-F2).
    effect(() => {
      const blobId = this.previewBlobId();
      const element = this.frame()?.nativeElement;
      if (element && blobId) {
        element.src = this.ingestApi.previewUrl(blobId);
      }
    });
  }
}
