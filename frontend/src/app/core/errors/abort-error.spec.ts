import { describe, expect, it } from 'vitest';
import { isAbortError } from './abort-error';

describe('isAbortError', () => {
  it('detects native AbortError', () => {
    expect(isAbortError(new DOMException('The user aborted a request.', 'AbortError'))).toBe(true);
  });

  it('detects wrapped fetch abort', () => {
    expect(isAbortError({ error: { name: 'AbortError', message: 'The user aborted a request.' } })).toBe(
      true
    );
  });

  it('ignores unrelated errors', () => {
    expect(isAbortError(new TypeError("Cannot read properties of undefined (reading 'sentence')"))).toBe(
      false
    );
  });
});
