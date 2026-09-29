/** Fetch/HttpClient cancellation (navigation, destroyed subscriber, view transition). */
export function isAbortError(error: unknown): boolean {
  let current: unknown = error;
  const seen = new Set<unknown>();

  while (current && typeof current === 'object' && !seen.has(current)) {
    seen.add(current);
    const name = 'name' in current ? String((current as { name?: unknown }).name ?? '') : '';
    const message =
      'message' in current ? String((current as { message?: unknown }).message ?? '') : '';
    if (
      name === 'AbortError' ||
      message.includes('The user aborted a request') ||
      message.includes('signal is aborted')
    ) {
      return true;
    }
    current =
      (current as { rejection?: unknown }).rejection ??
      (current as { error?: unknown }).error ??
      (current as { cause?: unknown }).cause ??
      (current as { ngOriginalError?: unknown }).ngOriginalError;
  }

  return false;
}
