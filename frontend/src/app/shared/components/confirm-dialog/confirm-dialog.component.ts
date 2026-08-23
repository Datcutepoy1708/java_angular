import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';

export type DialogVariant = 'danger' | 'primary' | 'warning' | 'success';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  templateUrl: './confirm-dialog.component.html',
  styleUrl: './confirm-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialogComponent {
  // Inputs
  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('Bạn có chắc chắn muốn xóa không?');
  readonly message = input<string>('');
  readonly warningMessage = input<string | null>(null);
  readonly confirmText = input<string>('Chắc chắn, xóa ngay');
  readonly cancelText = input<string>('Hủy bỏ');
  readonly variant = input<DialogVariant>('danger');
  readonly loading = input<boolean>(false);
  readonly loadingText = input<string>('Đang xử lý...');

  // Outputs
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  // Backdrop safety flag to avoid accidental close when dragging text
  private isMouseDownOnBackdrop = false;

  onBackdropMouseDown(event: MouseEvent): void {
    this.isMouseDownOnBackdrop = event.target === event.currentTarget;
  }

  onBackdropMouseUp(event: MouseEvent): void {
    if (this.isMouseDownOnBackdrop && event.target === event.currentTarget) {
      this.cancelled.emit();
    }
    this.isMouseDownOnBackdrop = false;
  }

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
