import { TestBed } from '@angular/core/testing';
import { StatusBar } from './status-bar';

describe('StatusBar', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({ imports: [StatusBar] }).compileComponents();
    const fixture = TestBed.createComponent(StatusBar);
    fixture.componentRef.setInput('attributeCount', 3);
    fixture.componentRef.setInput('criticalCount', 2);
    fixture.componentRef.setInput('toReviewCount', 1);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the attribute/critical/to-review counts', async () => {
    const fixture = await createComponent();
    const summary = (fixture.nativeElement as HTMLElement).querySelector(
      '.status-bar__summary',
    )!.textContent;
    expect(summary).toContain('3');
    expect(summary).toContain('2');
    expect(summary).toContain('1');
  });

  it('emits rejectRequested when "Ablehnen" is clicked', async () => {
    const fixture = await createComponent();
    const spy = vi.fn();
    fixture.componentInstance.rejectRequested.subscribe(spy);
    const button = (fixture.nativeElement as HTMLElement).querySelector(
      '.btn--secondary',
    ) as HTMLButtonElement;
    button.click();
    expect(spy).toHaveBeenCalledOnce();
  });

  it('keeps "Bestätigen" enabled even if attributes are still listed as to review', async () => {
    const fixture = await createComponent();
    const confirmButton = (fixture.nativeElement as HTMLElement).querySelector(
      '.btn--primary',
    ) as HTMLButtonElement;
    expect(confirmButton.disabled).toBe(false);
  });

  it('emits confirmRequested when "Bestätigen" is clicked', async () => {
    const fixture = await createComponent();
    const spy = vi.fn();
    fixture.componentInstance.confirmRequested.subscribe(spy);
    const confirmButton = (fixture.nativeElement as HTMLElement).querySelector(
      '.btn--primary',
    ) as HTMLButtonElement;
    expect(confirmButton.disabled).toBe(false);
    confirmButton.click();
    expect(spy).toHaveBeenCalledOnce();
  });

  it('emits checkRequested from the "Im DMS prüfen" button next to the other actions', async () => {
    const fixture = await createComponent();
    const spy = vi.fn();
    fixture.componentInstance.checkRequested.subscribe(spy);
    const button = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.status-bar__actions button'),
    ).find((b) => b.textContent?.includes('Im DMS prüfen')) as HTMLButtonElement;

    button.click();
    expect(spy).toHaveBeenCalledOnce();

    fixture.componentRef.setInput('checking', true);
    fixture.detectChanges();
    expect(button.disabled).toBe(true);

    fixture.componentRef.setInput('checking', false);
    fixture.componentRef.setInput('canCheck', false);
    fixture.detectChanges();
    expect(button.disabled).toBe(true);
  });

  it('disables both actions while busy', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput('toReviewCount', 0);
    fixture.componentRef.setInput('busy', true);
    fixture.detectChanges();
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
    buttons.forEach((button) => expect((button as HTMLButtonElement).disabled).toBe(true));
  });
});
