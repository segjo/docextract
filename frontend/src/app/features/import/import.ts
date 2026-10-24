import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { IngestApi } from '../../core/api/ingest-api';
import { DmsRepository } from '../../core/models/ingest.model';

@Component({
  selector: 'app-import',
  templateUrl: './import.html',
  styleUrl: './import.scss',
})
export class Import implements OnInit {
  private readonly ingestApi = inject(IngestApi);
  private readonly router = inject(Router);

  protected readonly dragOver = signal(false);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly validationError = signal<string | null>(null);
  protected readonly uploading = signal(false);
  protected readonly uploadError = signal<string | null>(null);

  protected readonly acceptedFileExtensions = signal<string[]>([]);
  protected readonly maxFileSizeBytes = signal<number | null>(null);
  protected readonly repositories = signal<DmsRepository[]>([]);
  protected readonly selectedRepositoryId = signal<string | null>(null);
  protected readonly importInfoError = signal<string | null>(null);

  protected get acceptAttribute(): string {
    return this.acceptedFileExtensions().join(',');
  }

  protected fileExtensionLabel(file: File): string {
    return (file.name.split('.').pop() ?? '').toUpperCase();
  }

  protected fileSizeLabel(file: File): string {
    const kb = file.size / 1024;
    return kb < 1024 ? `${Math.round(kb)} KB` : `${(kb / 1024).toFixed(1)} MB`;
  }

  ngOnInit(): void {
    this.ingestApi.importInfo().subscribe({
      next: (info) => {
        this.acceptedFileExtensions.set(info.acceptedFileExtensions);
        this.maxFileSizeBytes.set(info.maxFileSizeBytes);
        this.repositories.set(info.repositories);
        const defaultRepository = info.repositories.find((repo) => repo.isDefault);
        this.selectedRepositoryId.set((defaultRepository ?? info.repositories[0])?.id ?? null);
      },
      error: () => {
        this.importInfoError.set(
          'Import-Informationen konnten nicht geladen werden. Bitte Seite neu laden.',
        );
      },
    });
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  protected onDragLeave(): void {
    this.dragOver.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.item(0) ?? null;
    this.selectFile(file);
  }

  protected onFileInputChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.item(0) ?? null;
    this.selectFile(file);
  }

  protected onRepositoryChange(event: Event): void {
    this.selectedRepositoryId.set((event.target as HTMLSelectElement).value);
  }

  private selectFile(file: File | null): void {
    this.uploadError.set(null);
    if (!file) {
      this.selectedFile.set(null);
      this.validationError.set(null);
      return;
    }
    const extension = `.${file.name.split('.').pop()?.toLowerCase() ?? ''}`;
    if (!this.acceptedFileExtensions().includes(extension)) {
      this.selectedFile.set(null);
      this.validationError.set(
        `Dateityp ${extension} wird nicht unterstuetzt. Erlaubt: ${this.acceptedFileExtensions().join(', ')}`,
      );
      return;
    }
    const maxSize = this.maxFileSizeBytes();
    if (maxSize !== null && file.size > maxSize) {
      this.selectedFile.set(null);
      this.validationError.set(
        `Datei ueberschreitet die maximale Groesse von ${Math.round(maxSize / (1024 * 1024))} MB.`,
      );
      return;
    }
    this.validationError.set(null);
    this.selectedFile.set(file);
  }

  protected cancel(): void {
    this.selectedFile.set(null);
    this.validationError.set(null);
    this.uploadError.set(null);
  }

  protected submit(): void {
    const file = this.selectedFile();
    if (!file || this.uploading()) {
      return;
    }
    this.uploading.set(true);
    this.uploadError.set(null);
    this.ingestApi.upload(file, this.selectedRepositoryId()).subscribe({
      next: (ingested) => {
        this.uploading.set(false);
        this.router.navigate(['/processes', ingested.processId]);
      },
      error: () => {
        this.uploading.set(false);
        this.uploadError.set('Import fehlgeschlagen. Bitte erneut versuchen.');
      },
    });
  }
}
