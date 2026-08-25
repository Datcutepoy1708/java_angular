import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BannerService } from '../../../core/services/banner.service';
import { UploadService } from '../../../core/services/upload.service';
import { Banner, BannerPosition, BannerRequest, BannerStatus } from '../../../core/models/banner.model';
import { ImageUploadComponent } from '../../../shared/components/image-upload/image-upload.component';

@Component({
  selector: 'app-banner-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe, ImageUploadComponent],
  templateUrl: './banner-manage.component.html',
  styleUrl: './banner-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BannerManageComponent implements OnInit {
  private readonly bannerService = inject(BannerService);
  private readonly uploadService = inject(UploadService);
  private readonly fb = inject(FormBuilder);

  readonly banners = signal<Banner[]>([]);
  readonly filterPosition = signal<BannerPosition | ''>('');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly uploadingImage = signal<boolean>(false);
  readonly imagePreview = signal<string | null>(null);

  // Modal State
  readonly showModal = signal<boolean>(false);
  readonly isEditing = signal<boolean>(false);
  readonly editingBannerId = signal<number | null>(null);

  // Delete State
  readonly showDeleteModal = signal<boolean>(false);
  readonly deletingBannerId = signal<number | null>(null);
  readonly deletingTitle = signal<string>('');

  readonly bannerForm: FormGroup = this.fb.group({
    title: ['', [Validators.maxLength(200)]],
    imageUrl: ['', [Validators.required, Validators.maxLength(500)]],
    linkUrl: ['', [Validators.maxLength(500)]],
    position: ['homepage_slider', [Validators.required]],
    sortOrder: [0, [Validators.min(0)]],
    startDate: [''],
    endDate: [''],
    status: ['active', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadBanners();
  }

  loadBanners(): void {
    this.loading.set(true);
    const pos = this.filterPosition() ? (this.filterPosition() as BannerPosition) : undefined;
    this.bannerService.getAdminBanners(pos).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.banners.set(res.data);
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading banners:', err);
        this.loading.set(false);
      },
    });
  }

  onFilterPosition(pos: string): void {
    this.filterPosition.set(pos as BannerPosition | '');
    this.loadBanners();
  }

  openCreateModal(): void {
    this.isEditing.set(false);
    this.editingBannerId.set(null);
    this.imagePreview.set(null);
    this.bannerForm.reset({
      title: '',
      imageUrl: '',
      linkUrl: '',
      position: 'homepage_slider',
      sortOrder: 0,
      startDate: '',
      endDate: '',
      status: 'active',
    });
    this.showModal.set(true);
  }

  openEditModal(banner: Banner): void {
    this.isEditing.set(true);
    this.editingBannerId.set(banner.bannerId);
    this.imagePreview.set(banner.imageUrl || null);

    // Format ISO string to datetime-local format YYYY-MM-DDThh:mm
    const fmtDate = (d?: string) => (d ? d.slice(0, 16) : '');

    this.bannerForm.patchValue({
      title: banner.title || '',
      imageUrl: banner.imageUrl,
      linkUrl: banner.linkUrl || '',
      position: banner.position,
      sortOrder: banner.sortOrder,
      startDate: fmtDate(banner.startDate),
      endDate: fmtDate(banner.endDate),
      status: banner.status,
    });
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  handleImageUpload(file: File): void {
    const reader = new FileReader();
    reader.onload = (e) => {
      this.imagePreview.set(e.target?.result as string);
    };
    reader.readAsDataURL(file);

    this.uploadingImage.set(true);
    this.uploadService.uploadImage(file).subscribe({
      next: (url) => {
        this.bannerForm.patchValue({ imageUrl: url });
        this.imagePreview.set(url);
        this.uploadingImage.set(false);
      },
      error: (err) => {
        console.error('Upload error:', err);
        alert('Tải ảnh banner lên máy chủ thất bại. Bạn có thể nhập link ảnh trực tiếp.');
        this.uploadingImage.set(false);
      },
    });
  }

  onImageUrlChange(url: string): void {
    this.bannerForm.patchValue({ imageUrl: url });
    this.imagePreview.set(url || null);
  }

  onImageRemoved(): void {
    this.bannerForm.patchValue({ imageUrl: '' });
    this.imagePreview.set(null);
  }

  saveBanner(): void {
    if (this.bannerForm.invalid) {
      this.bannerForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const val = this.bannerForm.value;
    const req: BannerRequest = {
      title: val.title ? val.title.trim() : undefined,
      imageUrl: val.imageUrl.trim(),
      linkUrl: val.linkUrl ? val.linkUrl.trim() : undefined,
      position: val.position,
      sortOrder: Number(val.sortOrder) || 0,
      startDate: val.startDate ? new Date(val.startDate).toISOString() : undefined,
      endDate: val.endDate ? new Date(val.endDate).toISOString() : undefined,
      status: val.status,
    };

    if (this.isEditing() && this.editingBannerId()) {
      this.bannerService.updateBanner(this.editingBannerId()!, req).subscribe({
        next: () => {
          this.saving.set(false);
          this.closeModal();
          this.loadBanners();
        },
        error: (err) => {
          console.error('Error updating banner:', err);
          this.saving.set(false);
        },
      });
    } else {
      this.bannerService.createBanner(req).subscribe({
        next: () => {
          this.saving.set(false);
          this.closeModal();
          this.loadBanners();
        },
        error: (err) => {
          console.error('Error creating banner:', err);
          this.saving.set(false);
        },
      });
    }
  }

  toggleStatus(banner: Banner): void {
    const newStatus: BannerStatus = banner.status === 'active' ? 'inactive' : 'active';
    const req: BannerRequest = {
      title: banner.title,
      imageUrl: banner.imageUrl,
      linkUrl: banner.linkUrl,
      position: banner.position,
      sortOrder: banner.sortOrder,
      startDate: banner.startDate,
      endDate: banner.endDate,
      status: newStatus,
    };

    this.bannerService.updateBanner(banner.bannerId, req).subscribe({
      next: () => this.loadBanners(),
      error: (err) => console.error('Error toggling banner status:', err),
    });
  }

  openDeleteModal(banner: Banner): void {
    this.deletingBannerId.set(banner.bannerId);
    this.deletingTitle.set(banner.title || `Banner #${banner.bannerId}`);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deletingBannerId.set(null);
  }

  confirmDelete(): void {
    const id = this.deletingBannerId();
    if (!id) return;

    this.bannerService.deleteBanner(id).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.loadBanners();
      },
      error: (err) => {
        console.error('Error deleting banner:', err);
        this.closeDeleteModal();
      },
    });
  }
}
