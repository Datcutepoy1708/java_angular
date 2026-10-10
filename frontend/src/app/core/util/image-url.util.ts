import { environment } from '../../../environments/environment';

/**
 * Normalizes an image URL to work across both development and production environments.
 * - Strips any hardcoded "http://localhost:8080" prefix.
 * - On production (where environment.apiUrl is ''), returns relative path '/uploads/...'.
 * - On local development (where environment.apiUrl is 'http://localhost:8080'), prepends environment.apiUrl.
 */
export function normalizeImageUrl(url?: string | null): string {
  if (!url || !url.trim()) return '';
  const trimmed = url.trim();

  // Strip localhost:8080 or http://localhost:8080 if present
  let clean = trimmed.replace(/^https?:\/\/localhost:8080\/?/, '/');

  if (clean.startsWith('uploads/')) {
    clean = '/' + clean;
  }

  if (clean.startsWith('/uploads/')) {
    return environment.apiUrl ? `${environment.apiUrl}${clean}` : clean;
  }

  return clean;
}
