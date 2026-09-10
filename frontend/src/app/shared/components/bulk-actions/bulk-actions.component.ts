import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { BulkOperation, BulkSelection } from './bulk-selection';

@Component({
  selector: 'app-bulk-actions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!-- Inline Bulk Action Bar (matches product-manage style) -->
    @if (selection().count() > 0) {
      <div class="bulk-action-bar" role="region" aria-label="Thao tác hàng loạt">
        <span class="bulk-count">
          Đã chọn <strong>{{ selection().count() }}</strong> mục
        </span>

        <div class="bulk-btns">
          @for (operation of operations(); track operation.label) {
            <button
              type="button"
              class="btn btn-sm"
              [class.btn-ghost-danger]="operation.variant === 'danger'"
              [class.btn-warning]="operation.variant === 'warning'"
              [class.btn-success]="operation.variant === 'success'"
              [class.btn-primary]="operation.variant === 'info' || operation.variant === 'default' || !operation.variant"
              [disabled]="disabled() || selection().busy()"
              (click)="openConfirm(operation)"
            >
              {{ operation.label }}
            </button>
          }

          <button
            type="button"
            class="btn btn-sm btn-ghost"
            [disabled]="selection().busy()"
            (click)="selection().clear()"
          >
            Bỏ chọn
          </button>
        </div>
      </div>
    }

    <!-- Result / Report Banner -->
    @if (selection().report()) {
      <div
        class="bulk-report-bar"
        [class.bulk-report-bar--error]="selection().failures().length > 0"
        role="status"
      >
        <div class="report-content">
          <span>{{ selection().report() }}</span>
          @if (selection().failures().length > 0) {
            <ul class="failures-list">
              @for (f of selection().failures(); track f.id) {
                <li>Mục #{{ f.id }}: {{ f.message }}</li>
              }
            </ul>
          }
        </div>
        <button type="button" class="btn-close-report" (click)="clearReport()" title="Đóng thông báo">
          ✕
        </button>
      </div>
    }

    <!-- Modal Confirm Dialog (centered overlay, matches app-confirm-dialog) -->
    @if (pending(); as op) {
      <div
        class="modal-backdrop"
        (mousedown)="onBackdropMouseDown($event)"
        (mouseup)="onBackdropMouseUp($event)"
      >
        <div class="confirm-dialog" (click)="$event.stopPropagation()" role="alertdialog" aria-modal="true">
          <!-- Icon circle -->
          <div
            class="confirm-icon-wrap"
            [class.confirm-icon-wrap--danger]="op.variant === 'danger'"
            [class.confirm-icon-wrap--warning]="op.variant === 'warning'"
            [class.confirm-icon-wrap--success]="op.variant === 'success'"
            [class.confirm-icon-wrap--primary]="op.variant !== 'danger' && op.variant !== 'warning' && op.variant !== 'success'"
          >
            @if (op.variant === 'danger') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="dialog-icon">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            } @else if (op.variant === 'warning') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="dialog-icon">
                <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            } @else {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="dialog-icon">
                <path d="M3 12a9 9 0 019-9 9.75 9.75 0 016.74 2.74L21 8"/>
                <path d="M21 3v5h-5"/>
                <path d="M21 12a9 9 0 01-9 9 9.75 9.75 0 01-6.74-2.74L3 16"/>
                <path d="M3 21v-5h5"/>
              </svg>
            }
          </div>

          <!-- Title & Body -->
          <h4 class="confirm-title">
            Xác nhận {{ op.label.toLowerCase() }}?
          </h4>

          <div class="confirm-body">
            <p class="confirm-msg">
              Bạn có chắc chắn muốn thực hiện thao tác <strong>"{{ op.label }}"</strong> cho
              <strong>{{ selection().count() }}</strong> mục đã chọn không?
            </p>

            @if (op.requiresNote) {
              <div class="confirm-note-group">
                <label for="bulk-note-input" class="note-label">
                  Lý do / Ghi chú <span class="required">*</span>
                </label>
                <input
                  id="bulk-note-input"
                  type="text"
                  class="note-input"
                  placeholder="Nhập lý do thực hiện..."
                  maxlength="500"
                  [value]="note()"
                  [disabled]="selection().busy()"
                  (input)="onNoteInput($event)"
                />
              </div>
            }
          </div>

          <!-- Action Buttons -->
          <div class="confirm-actions">
            <button
              type="button"
              class="btn btn-ghost"
              [disabled]="selection().busy()"
              (click)="pending.set(null)"
            >
              Hủy bỏ
            </button>

            <button
              type="button"
              class="btn"
              [class.btn-danger]="op.variant === 'danger'"
              [class.btn-warning]="op.variant === 'warning'"
              [class.btn-success]="op.variant === 'success'"
              [class.btn-primary]="op.variant !== 'danger' && op.variant !== 'warning' && op.variant !== 'success'"
              [disabled]="selection().busy() || (op.requiresNote && !note().trim())"
              (click)="confirm(op)"
            >
              @if (selection().busy()) {
                <span class="btn-spinner"></span> Đang xử lý...
              } @else {
                Xác nhận
              }
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    :host {
      display: block;
    }

    /* ─── Inline Bulk Action Bar (Exact product-manage design) ─── */
    .bulk-action-bar {
      margin-top: 12px;
      margin-bottom: 12px;
      padding: 10px 14px;
      background: #EEF3FF;
      border: 1px solid #bfdbfe;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      animation: fadeIn 0.15s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .bulk-count {
      font-size: 0.825rem;
      color: #1e40af;

      strong {
        font-weight: 700;
      }
    }

    .bulk-btns {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }

    /* ─── Standard Action Buttons ─── */
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 7px 14px;
      font-size: 0.8125rem;
      font-weight: 500;
      line-height: 1.2;
      border-radius: 6px;
      border: 1px solid transparent;
      cursor: pointer;
      font-family: inherit;
      transition: all 0.15s ease;
      white-space: nowrap;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }

    .btn-sm {
      padding: 5px 12px;
      font-size: 0.8rem;
    }

    .btn-primary {
      background: #2563eb;
      color: #ffffff;
      border-color: #2563eb;

      &:hover:not(:disabled) {
        background: #1d4ed8;
        border-color: #1d4ed8;
      }
    }

    .btn-danger {
      background: #ef4444;
      color: #ffffff;
      border-color: #ef4444;

      &:hover:not(:disabled) {
        background: #dc2626;
        border-color: #dc2626;
      }
    }

    .btn-ghost-danger {
      background: #ffffff;
      color: #dc2626;
      border-color: #fca5a5;

      &:hover:not(:disabled) {
        background: #fef2f2;
        border-color: #f87171;
      }
    }

    .btn-warning {
      background: #f59e0b;
      color: #ffffff;
      border-color: #f59e0b;

      &:hover:not(:disabled) {
        background: #d97706;
        border-color: #d97706;
      }
    }

    .btn-success {
      background: #10b981;
      color: #ffffff;
      border-color: #10b981;

      &:hover:not(:disabled) {
        background: #059669;
        border-color: #059669;
      }
    }

    .btn-ghost {
      background: #ffffff;
      color: #475569;
      border-color: #cbd5e1;

      &:hover:not(:disabled) {
        background: #f8fafc;
        color: #1e293b;
      }
    }

    /* ─── Report Banner ─── */
    .bulk-report-bar {
      margin-top: 10px;
      margin-bottom: 10px;
      padding: 10px 14px;
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: 8px;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      font-size: 0.825rem;
      color: #166534;

      &.bulk-report-bar--error {
        background: #fef2f2;
        border-color: #fecaca;
        color: #991b1b;
      }
    }

    .report-content {
      flex: 1;

      .failures-list {
        margin: 6px 0 0 16px;
        padding: 0;
        font-size: 0.78rem;
      }
    }

    .btn-close-report {
      background: none;
      border: none;
      color: inherit;
      opacity: 0.6;
      cursor: pointer;
      padding: 0 4px;
      font-size: 0.9rem;
      line-height: 1;

      &:hover { opacity: 1; }
    }

    /* ─── Modal Backdrop & Confirm Dialog ─── */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 1050;
      background: rgba(15, 23, 42, 0.45);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      animation: modalFadeIn 0.18s ease-out;
    }

    @keyframes modalFadeIn {
      from { opacity: 0; }
      to   { opacity: 1; }
    }

    .confirm-dialog {
      background: #ffffff;
      border-radius: 14px;
      padding: 24px;
      max-width: 420px;
      width: 100%;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08);
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      animation: modalScaleUp 0.18s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes modalScaleUp {
      from { opacity: 0; transform: scale(0.95); }
      to   { opacity: 1; transform: scale(1); }
    }

    .confirm-icon-wrap {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 14px;

      &.confirm-icon-wrap--danger {
        background: #fee2e2;
        color: #dc2626;
      }

      &.confirm-icon-wrap--warning {
        background: #fef3c7;
        color: #d97706;
      }

      &.confirm-icon-wrap--success {
        background: #d1fae5;
        color: #059669;
      }

      &.confirm-icon-wrap--primary {
        background: #dbeafe;
        color: #2563eb;
      }
    }

    .dialog-icon {
      width: 24px;
      height: 24px;
    }

    .confirm-title {
      margin: 0 0 8px;
      font-size: 1.1rem;
      font-weight: 600;
      color: #0f172a;
    }

    .confirm-body {
      width: 100%;
      margin-bottom: 20px;
    }

    .confirm-msg {
      margin: 0;
      font-size: 0.875rem;
      line-height: 1.5;
      color: #475569;

      strong {
        color: #0f172a;
      }
    }

    .confirm-note-group {
      margin-top: 14px;
      text-align: left;
    }

    .note-label {
      display: block;
      font-size: 0.8rem;
      font-weight: 600;
      color: #334155;
      margin-bottom: 6px;

      .required {
        color: #dc2626;
      }
    }

    .note-input {
      width: 100%;
      padding: 8px 12px;
      border: 1px solid #cbd5e1;
      border-radius: 6px;
      font-size: 0.85rem;
      outline: none;
      font-family: inherit;
      box-sizing: border-box;
      transition: border-color 0.15s ease;

      &:focus {
        border-color: #2563eb;
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
      }
    }

    .confirm-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 10px;
      width: 100%;
    }

    .btn-spinner {
      display: inline-block;
      width: 13px;
      height: 13px;
      border: 2px solid rgba(255, 255, 255, 0.4);
      border-top-color: #ffffff;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `],
})
export class BulkActionsComponent {
  readonly selection = input.required<BulkSelection>();
  readonly operations = input<BulkOperation[]>([]);
  readonly disabled = input<boolean>(false);
  readonly completed = output<void>();

  readonly pending = signal<BulkOperation | null>(null);
  readonly note = signal<string>('');

  private isMouseDownOnBackdrop = false;

  openConfirm(op: BulkOperation): void {
    this.note.set('');
    this.pending.set(op);
  }

  onBackdropMouseDown(event: MouseEvent): void {
    this.isMouseDownOnBackdrop = event.target === event.currentTarget;
  }

  onBackdropMouseUp(event: MouseEvent): void {
    if (this.isMouseDownOnBackdrop && event.target === event.currentTarget) {
      if (!this.selection().busy()) {
        this.pending.set(null);
      }
    }
    this.isMouseDownOnBackdrop = false;
  }

  onNoteInput(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    this.note.set(inputEl.value);
  }

  clearReport(): void {
    this.selection().report.set('');
    this.selection().failures.set([]);
  }

  async confirm(op: BulkOperation): Promise<void> {
    const currentNote = this.note();
    await this.selection().execute(op, currentNote);
    this.pending.set(null);
    this.completed.emit();
  }
}
