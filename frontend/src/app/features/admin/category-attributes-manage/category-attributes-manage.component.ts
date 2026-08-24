import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AttributeService } from '../../../core/services/attribute.service';
import { CategoryService } from '../../../core/services/category.service';
import { AttributeDataType, AttributeRequest, AttributeResponse } from '../../../core/models/attribute.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { ConfirmDialogComponent } from '../../../shared';

@Component({
  selector: 'app-category-attributes-manage',
  imports: [CommonModule, ReactiveFormsModule, ConfirmDialogComponent],
  templateUrl: './category-attributes-manage.component.html',
  styleUrl: './category-attributes-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryAttributesManageComponent implements OnInit {
  private readonly attributeService = inject(AttributeService);
  private readonly categoryService = inject(CategoryService);
  private readonly fb = inject(FormBuilder);

  readonly categories = signal<CategoryResponse[]>([]);
  readonly selectedCategoryId = signal<number | null>(null);
  readonly attributes = signal<AttributeResponse[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);

  // Modal State
  readonly isModalOpen = signal(false);
  readonly modalMode = signal<'create' | 'edit'>('create');
  readonly selectedAttributeId = signal<number | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // Confirm Delete Dialog
  readonly isConfirmOpen = signal(false);
  readonly deletingAttribute = signal<AttributeResponse | null>(null);

  readonly attributeForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    dataType: ['text' as AttributeDataType, [Validators.required]],
    unit: ['', [Validators.maxLength(50)]],
    sortOrder: [0, [Validators.min(0)]]
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.categoryService.getAll().subscribe({
      next: (res: { data: CategoryResponse[]; success: boolean }) => {
        if (res.success && res.data) {
          this.categories.set(res.data);
          if (res.data.length > 0 && !this.selectedCategoryId()) {
            this.onSelectCategory(res.data[0].categoryId);
          }
        }
      }
    });
  }

  onSelectCategory(categoryId: number): void {
    this.selectedCategoryId.set(categoryId);
    this.loadAttributes(categoryId);
  }

  loadAttributes(categoryId: number): void {
    this.loading.set(true);
    this.attributeService.getByCategory(categoryId).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.success && res.data) {
          this.attributes.set(res.data);
        }
      },
      error: () => {
        this.loading.set(false);
        this.attributes.set([]);
      }
    });
  }

  openCreateModal(): void {
    if (!this.selectedCategoryId()) return;
    this.modalMode.set('create');
    this.selectedAttributeId.set(null);
    this.attributeForm.reset({
      name: '',
      dataType: 'text',
      unit: '',
      sortOrder: this.attributes().length + 1
    });
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(attr: AttributeResponse): void {
    this.modalMode.set('edit');
    this.selectedAttributeId.set(attr.attributeId);
    this.attributeForm.patchValue({
      name: attr.name,
      dataType: attr.dataType,
      unit: attr.unit || '',
      sortOrder: attr.sortOrder
    });
    this.errorMessage.set(null);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.errorMessage.set(null);
  }

  saveAttribute(): void {
    if (this.attributeForm.invalid || !this.selectedCategoryId()) {
      this.attributeForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    const formVal = this.attributeForm.value;
    const request: AttributeRequest = {
      categoryId: this.selectedCategoryId()!,
      name: formVal.name.trim(),
      dataType: formVal.dataType,
      unit: formVal.unit ? formVal.unit.trim() : undefined,
      sortOrder: Number(formVal.sortOrder) || 0
    };

    if (this.modalMode() === 'create') {
      this.attributeService.create(request).subscribe({
        next: (res) => {
          this.saving.set(false);
          if (res.success) {
            this.showToast('Thêm thuộc tính thành công!');
            this.closeModal();
            this.loadAttributes(this.selectedCategoryId()!);
          }
        },
        error: (err) => {
          this.saving.set(false);
          this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi tạo thuộc tính.');
        }
      });
    } else {
      const id = this.selectedAttributeId()!;
      this.attributeService.update(id, request).subscribe({
        next: (res) => {
          this.saving.set(false);
          if (res.success) {
            this.showToast('Cập nhật thuộc tính thành công!');
            this.closeModal();
            this.loadAttributes(this.selectedCategoryId()!);
          }
        },
        error: (err) => {
          this.saving.set(false);
          this.errorMessage.set(err.error?.message || 'Có lỗi xảy ra khi cập nhật thuộc tính.');
        }
      });
    }
  }

  confirmDelete(attr: AttributeResponse): void {
    this.deletingAttribute.set(attr);
    this.isConfirmOpen.set(true);
  }

  onDeleteConfirmed(): void {
    const attr = this.deletingAttribute();
    if (!attr) return;

    this.attributeService.delete(attr.attributeId).subscribe({
      next: (res) => {
        this.isConfirmOpen.set(false);
        this.deletingAttribute.set(null);
        if (res.success) {
          this.showToast(`Đã xóa thuộc tính "${attr.name}" thành công!`);
          this.loadAttributes(this.selectedCategoryId()!);
        }
      },
      error: (err) => {
        this.isConfirmOpen.set(false);
        this.showToast(err.error?.message || 'Không thể xóa thuộc tính này.', true);
      }
    });
  }

  onDeleteCancelled(): void {
    this.isConfirmOpen.set(false);
    this.deletingAttribute.set(null);
  }

  private showToast(msg: string, isError = false): void {
    if (isError) {
      this.errorMessage.set(msg);
      setTimeout(() => this.errorMessage.set(null), 4000);
    } else {
      this.successMessage.set(msg);
      setTimeout(() => this.successMessage.set(null), 3500);
    }
  }
}
