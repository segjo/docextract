import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { axe } from 'vitest-axe';
import { Import } from './import';

describe('Import a11y', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Import],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  it('hat keine axe-Verstoesse mit Impact critical/serious', async () => {
    const fixture = TestBed.createComponent(Import);
    fixture.detectChanges();
    const results = await axe(fixture.nativeElement as HTMLElement, {
      resultTypes: ['violations'],
    });
    const relevant = results.violations.filter((violation) =>
      ['critical', 'serious'].includes(violation.impact ?? ''),
    );
    expect(relevant).toEqual([]);
  });
});
