import { HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { map } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Automatically intercepts incoming HTTP responses and sanitizes any image URLs
 * containing localhost:8080/uploads to prevent Mixed Content errors on production.
 */
export const imageUrlInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  return next(req).pipe(
    map((event: HttpEvent<unknown>) => {
      if (event instanceof HttpResponse && event.body && typeof event.body === 'object') {
        try {
          const raw = JSON.stringify(event.body);
          let modified = false;
          let newRaw = raw;

          // Replace hardcoded localhost:8080/uploads with proper path
          if (raw.includes('http://localhost:8080/uploads')) {
            const replacement = environment.apiUrl ? `${environment.apiUrl}/uploads` : '/uploads';
            newRaw = newRaw.replaceAll('http://localhost:8080/uploads', replacement);
            modified = true;
          }

          // In dev mode, ensure relative /uploads/ URLs are fetched from backend port 8080
          if (environment.apiUrl && newRaw.includes('"/uploads/')) {
            newRaw = newRaw.replaceAll('"/uploads/', `"${environment.apiUrl}/uploads/`);
            modified = true;
          }

          if (modified) {
            return event.clone({ body: JSON.parse(newRaw) });
          }
        } catch {
          // If JSON stringify/parse fails, return original event safely
        }
      }
      return event;
    })
  );
};
