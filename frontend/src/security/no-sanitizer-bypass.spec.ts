import { readFileSync } from 'node:fs';
import { glob } from 'glob';

// NfA-F2: Angular escapes interpolation by default — any bypass/innerHTML is a potential XSS,
// since document content/LLM excerpts rendered in the validation UI count as untrusted data (T-1).
describe('security: keine Sanitizer-Bypaesse', () => {
  it('verwendet nirgends bypassSecurityTrust* oder [innerHTML]', () => {
    const offenders = glob
      .sync('src/**/*.{ts,html}')
      .filter((file) => file !== 'src/security/no-sanitizer-bypass.spec.ts')
      .filter((file) => /bypassSecurityTrust|\[innerHTML\]/.test(readFileSync(file, 'utf8')));
    expect(offenders).toEqual([]);
  });
});
