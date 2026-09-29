import { ErrorHandler, Injectable } from '@angular/core';
import { isAbortError } from './abort-error';

@Injectable()
export class AppErrorHandler extends ErrorHandler {
  override handleError(error: unknown): void {
    if (isAbortError(error)) {
      return;
    }
    super.handleError(error);
  }
}
