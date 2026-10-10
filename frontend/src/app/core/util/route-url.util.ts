/**
 * Utility to parse route URLs that may contain query strings or be external links,
 * ensuring Angular Router does not encode query parameters into the path segment.
 */
export interface ParsedRouteLink {
  path: string;
  queryParams?: Record<string, string>;
  isExternal: boolean;
}

export function parseRouteUrl(url?: string | null): ParsedRouteLink {
  if (!url || !url.trim()) {
    return { path: '/products', isExternal: false };
  }

  const trimmed = url.trim();

  // Handle external HTTP(S) URLs
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    try {
      const parsed = new URL(trimmed);
      if (typeof window !== 'undefined' && parsed.origin === window.location.origin) {
        const queryParams: Record<string, string> = {};
        parsed.searchParams.forEach((val, key) => {
          queryParams[key] = val;
        });
        return {
          path: parsed.pathname || '/products',
          queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined,
          isExternal: false
        };
      }
    } catch {
      // ignore parsing error and treat as external
    }
    return { path: trimmed, isExternal: true };
  }

  // Handle internal URLs with possible query parameters (e.g. "/products?category=laptop-gaming")
  const [pathPart, queryPart] = trimmed.split('?');
  const path = pathPart && pathPart.trim() ? pathPart.trim() : '/products';
  const queryParams: Record<string, string> = {};

  if (queryPart) {
    const searchParams = new URLSearchParams(queryPart);
    searchParams.forEach((val, key) => {
      queryParams[key] = val;
    });
  }

  return {
    path,
    queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined,
    isExternal: false
  };
}
