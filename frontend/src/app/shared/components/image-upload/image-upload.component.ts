import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';

@Component({
  selector: 'app-image-upload',
  standalone: true,
  templateUrl: './image-upload.component.html',
  styleUrl: './image-upload.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageUploadComponent {
  readonly imageUrl = input<string | null>(null);
  readonly label = input<string>('Ảnh / Icon');
  readonly helperText = input<string>('Hỗ trợ PNG, JPG, WEBP, SVG (tối đa 5MB)');
  readonly maxSizeMB = input<number>(5);
  readonly uploading = input<boolean>(false);
  readonly placeholderText = input<string>('https://example.com/image.png');

  readonly imageUrlChange = output<string>();
  readonly fileSelected = output<File>();
  readonly imageRemoved = output<void>();

  readonly inputMode = signal<'upload' | 'url'>('upload');

  setMode(mode: 'upload' | 'url'): void {
    this.inputMode.set(mode);
  }

  onFileChange(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    if (!inputEl.files || inputEl.files.length === 0) return;
    const file = inputEl.files[0];
    this.processFile(file);
    inputEl.value = ''; // reset so same file can be re-selected if needed
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    if (!event.dataTransfer?.files || event.dataTransfer.files.length === 0) return;
    const file = event.dataTransfer.files[0];
    this.processFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  private processFile(file: File): void {
    if (!file.type.startsWith('image/')) {
      alert('Vui lòng chọn một file ảnh hợp lệ (PNG, JPG, WEBP, SVG, GIF)');
      return;
    }

    if (file.size > this.maxSizeMB() * 1024 * 1024) {
      alert(`Dung lượng ảnh tối đa là ${this.maxSizeMB()}MB`);
      return;
    }

    this.fileSelected.emit(file);
  }

  onUrlChange(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const url = inputEl.value.trim();
    this.imageUrlChange.emit(url);
  }

  onRemove(): void {
    this.imageRemoved.emit();
  }
}
