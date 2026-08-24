import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ProductService } from '../../../../core/services/product.service';
import { CategoryService } from '../../../../core/services/category.service';
import { BrandService } from '../../../../core/services/brand.service';
import { UploadService } from '../../../../core/services/upload.service';
import { AttributeService } from '../../../../core/services/attribute.service';
import {
  ImageFormItem,
  ImageType,
  ProductImageResponse,
  ProductRequest,
  ProductStatus,
  ProductVariantResponse,
  VariantFormItem,
  VariantStatus,
} from '../../../../core/models/product.model';
import { CategoryResponse } from '../../../../core/models/category.model';
import { BrandResponse } from '../../../../core/models/brand.model';
import { AttributeResponse, ProductAttributeValueRequest } from '../../../../core/models/attribute.model';

@Component({
  selector: 'app-product-form',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductFormComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly brandService = inject(BrandService);
  private readonly uploadService = inject(UploadService);
  private readonly attributeService = inject(AttributeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  // ── Mode & IDs ────────────────────────────────────────────────
  readonly isEditMode = signal(false);
  readonly productId = signal<number | null>(null);

  // ── EAV Specifications State ─────────────────────────────────
  readonly availableAttributes = signal<AttributeResponse[]>([]);
  readonly attributeValues = signal<{ [attributeId: number]: string }>({});
  readonly loadingSpecs = signal(false);

  // ── Upload State ──────────────────────────────────────────────
  readonly uploadMode = signal<'file' | 'url'>('file');
  readonly isUploading = signal(false);
  readonly uploadError = signal<string | null>(null);
  readonly isDragging = signal(false);

  // ── Dropdown Options ──────────────────────────────────────────
  readonly categories = signal<CategoryResponse[]>([]);
  readonly brands = signal<BrandResponse[]>([]);

  // ── State ─────────────────────────────────────────────────────
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly saveMessage = signal<{ type: 'success' | 'warning' | 'error'; text: string } | null>(null);

  // ── Sub-tabs for nested items ─────────────────────────────────
  readonly variantTab = signal<'active' | 'trash'>('active');
  readonly imageTab = signal<'active' | 'trash'>('active');

  // ── Active Variants & Images Lists ────────────────────────────
  readonly variants = signal<VariantFormItem[]>([]);
  readonly images = signal<ImageFormItem[]>([]);

  // ── Deleted Variants & Images (Soft-deleted) ──────────────────
  readonly deletedVariants = signal<ProductVariantResponse[]>([]);
  readonly deletedImages = signal<ProductImageResponse[]>([]);

  // Image URL input field
  readonly newImageUrl = signal('');
  readonly newImageAlt = signal('');

  // Drag-and-drop index tracking
  private draggedImageIndex: number | null = null;

  // ── Main Product Form ─────────────────────────────────────────
  readonly form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(250)]],
    categoryId: [null, Validators.required],
    brandId: [null],
    sku: ['', Validators.maxLength(100)],
    shortDesc: ['', Validators.maxLength(500)],
    description: [''],
    warrantyMonths: [12, [Validators.min(0)]],
    status: ['active' as ProductStatus, Validators.required],
  });

  // ── Lifecycle ─────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadDropdowns();

    // Listen to category change to dynamically load attributes
    this.form.get('categoryId')?.valueChanges.subscribe((catId) => {
      if (catId) {
        this.loadCategoryAttributes(Number(catId), !this.loading());
      } else {
        this.availableAttributes.set([]);
        this.attributeValues.set({});
      }
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.productId.set(Number(idParam));
      this.loadProductData(Number(idParam));
      this.loadDeletedVariants(Number(idParam));
      this.loadDeletedImages(Number(idParam));
    } else {
      // Default with one initial variant
      this.addVariant();
    }
  }

  loadDropdowns(): void {
    this.categoryService.getAll().subscribe({
      next: (res) => this.categories.set(res.data),
    });
    this.brandService.getAll().subscribe({
      next: (res) => this.brands.set(res.data),
    });
  }

  loadCategoryAttributes(categoryId: number, resetValues = true): void {
    this.loadingSpecs.set(true);
    if (resetValues) {
      this.attributeValues.set({});
    }
    this.attributeService.getByCategory(categoryId).subscribe({
      next: (res) => {
        this.loadingSpecs.set(false);
        if (res.success && res.data) {
          this.availableAttributes.set(res.data);
        } else {
          this.availableAttributes.set([]);
        }
      },
      error: () => {
        this.loadingSpecs.set(false);
        this.availableAttributes.set([]);
      }
    });
  }

  onSpecChange(attributeId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.attributeValues.update((current) => ({
      ...current,
      [attributeId]: input.value
    }));
  }

  loadProductData(id: number): void {
    this.loading.set(true);
    this.productService.getById(id).subscribe({
      next: (res) => {
        const p = res.data;
        this.form.patchValue({
          name: p.name,
          categoryId: p.categoryId,
          brandId: p.brandId,
          sku: p.sku ?? '',
          shortDesc: p.shortDesc ?? '',
          description: p.description ?? '',
          warrantyMonths: p.warrantyMonths ?? 0,
          status: p.status,
        });

        if (p.categoryId) {
          this.loadCategoryAttributes(p.categoryId, false);
        }

        if (p.specifications && p.specifications.length > 0) {
          const valMap: { [attrId: number]: string } = {};
          for (const spec of p.specifications) {
            valMap[spec.attributeId] = spec.value;
          }
          this.attributeValues.set(valMap);
        } else {
          this.attributeService.getProductAttributes(id).subscribe({
            next: (specRes) => {
              if (specRes.success && specRes.data) {
                const valMap: { [attrId: number]: string } = {};
                for (const spec of specRes.data) {
                  valMap[spec.attributeId] = spec.value;
                }
                this.attributeValues.set(valMap);
              }
            }
          });
        }

        // Map variants
        if (p.variants && p.variants.length > 0) {
          this.variants.set(
            p.variants.map((v) => ({
              variantId: v.variantId,
              request: {
                variantName: v.variantName,
                skuVariant: v.skuVariant,
                price: v.price,
                salePrice: v.salePrice,
                costPrice: v.costPrice,
                status: v.status,
              },
              saveStatus: 'saved',
            }))
          );
        }

        // Map images
        if (p.images && p.images.length > 0) {
          this.images.set(
            p.images.map((img) => ({
              imageId: img.imageId,
              request: {
                imageUrl: img.imageUrl,
                imageType: img.imageType,
                altText: img.altText,
                sortOrder: img.sortOrder,
              },
              saveStatus: 'saved',
            }))
          );
        }

        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadDeletedVariants(productId: number): void {
    this.productService.getDeletedVariants(productId).subscribe({
      next: (res) => this.deletedVariants.set(res.data),
    });
  }

  loadDeletedImages(productId: number): void {
    this.productService.getDeletedImages(productId).subscribe({
      next: (res) => this.deletedImages.set(res.data),
    });
  }

  // ── Variant Actions ───────────────────────────────────────────
  addVariant(): void {
    this.variants.update((list) => [
      ...list,
      {
        variantId: null,
        request: {
          variantName: '',
          skuVariant: '',
          price: 0,
          salePrice: null,
          costPrice: null,
          status: 'active' as VariantStatus,
        },
        saveStatus: 'pending',
      },
    ]);
  }

  removeVariant(index: number): void {
    const item = this.variants()[index];
    if (item.variantId) {
      this.productService.softDeleteVariant(item.variantId).subscribe({
        next: () => {
          this.variants.update((list) => list.filter((_, i) => i !== index));
          if (this.productId()) {
            this.loadDeletedVariants(this.productId()!);
          }
        },
      });
    } else {
      this.variants.update((list) => list.filter((_, i) => i !== index));
    }
  }

  restoreVariant(variantId: number): void {
    this.productService.restoreVariant(variantId).subscribe({
      next: () => {
        if (this.productId()) {
          this.loadProductData(this.productId()!);
          this.loadDeletedVariants(this.productId()!);
        }
      },
    });
  }

  updateVariantField(index: number, field: string, value: any): void {
    this.variants.update((list) => {
      const copy = [...list];
      const target = { ...copy[index] };
      const req = { ...target.request, [field]: value };
      target.request = req;
      if (target.saveStatus === 'saved') {
        target.saveStatus = 'pending';
      }
      copy[index] = target;
      return copy;
    });
  }

  // ── Image Actions ─────────────────────────────────────────────
  onImageUrlInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.newImageUrl.set(input.value);
  }

  onImageAltInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.newImageAlt.set(input.value);
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.uploadFiles(input.files);
      input.value = ''; // Reset input
    }
  }

  onDragOverZone(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeaveZone(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.uploadFiles(event.dataTransfer.files);
    }
  }

  uploadFiles(files: FileList | File[]): void {
    if (!files || files.length === 0) return;

    this.uploadError.set(null);
    this.isUploading.set(true);

    const validFiles: File[] = [];
    const maxSizeBytes = 5 * 1024 * 1024; // 5MB

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (file.size > maxSizeBytes) {
        this.uploadError.set(`File "${file.name}" vượt quá dung lượng tối đa 5MB`);
        continue;
      }
      if (!file.type.startsWith('image/')) {
        this.uploadError.set(`File "${file.name}" không phải định dạng ảnh`);
        continue;
      }
      validFiles.push(file);
    }

    if (validFiles.length === 0) {
      this.isUploading.set(false);
      return;
    }

    const uploadObservables = validFiles.map((file) =>
      this.uploadService.uploadImage(file).pipe(
        catchError((err) => {
          console.error(`Lỗi tải file ${file.name}:`, err);
          return of(null);
        })
      )
    );

    forkJoin(uploadObservables).subscribe({
      next: (urls) => {
        urls.forEach((url, idx) => {
          if (url) {
            const currentImages = this.images();
            const isFirst = currentImages.length === 0;
            this.images.update((list) => [
              ...list,
              {
                imageId: null,
                request: {
                  imageUrl: url,
                  imageType: isFirst ? 'MAIN' : 'GALLERY',
                  altText: validFiles[idx]?.name ? validFiles[idx].name.replace(/\.[^/.]+$/, '') : null,
                  sortOrder: list.length,
                },
                saveStatus: 'pending',
              },
            ]);
          }
        });
        this.isUploading.set(false);
      },
      error: () => {
        this.uploadError.set('Có lỗi xảy ra khi tải ảnh lên máy chủ');
        this.isUploading.set(false);
      },
    });
  }

  addImage(): void {
    const url = this.newImageUrl().trim();
    if (!url) return;

    const currentImages = this.images();
    const isFirst = currentImages.length === 0;

    this.images.update((list) => [
      ...list,
      {
        imageId: null,
        request: {
          imageUrl: url,
          imageType: isFirst ? 'MAIN' : 'GALLERY',
          altText: this.newImageAlt().trim() || null,
          sortOrder: list.length,
        },
        saveStatus: 'pending',
      },
    ]);

    this.newImageUrl.set('');
    this.newImageAlt.set('');
  }

  removeImage(index: number): void {
    const item = this.images()[index];
    if (item.imageId) {
      this.productService.softDeleteImage(item.imageId).subscribe({
        next: () => {
          this.images.update((list) => list.filter((_, i) => i !== index));
          this.ensureMainImage();
          if (this.productId()) {
            this.loadDeletedImages(this.productId()!);
          }
        },
      });
    } else {
      this.images.update((list) => list.filter((_, i) => i !== index));
      this.ensureMainImage();
    }
  }

  restoreImage(imageId: number): void {
    this.productService.restoreImage(imageId).subscribe({
      next: () => {
        if (this.productId()) {
          this.loadProductData(this.productId()!);
          this.loadDeletedImages(this.productId()!);
        }
      },
    });
  }

  setMainImage(index: number): void {
    this.images.update((list) =>
      list.map((img, i) => ({
        ...img,
        request: {
          ...img.request,
          imageType: i === index ? ('MAIN' as ImageType) : ('GALLERY' as ImageType),
        },
        saveStatus: img.saveStatus === 'saved' ? 'pending' : img.saveStatus,
      }))
    );
  }

  private ensureMainImage(): void {
    const current = this.images();
    if (current.length > 0 && !current.some((img) => img.request.imageType === 'MAIN')) {
      this.setMainImage(0);
    }
  }

  // ── Drag & Drop for Image Reordering ──────────────────────────
  onDragStart(index: number): void {
    this.draggedImageIndex = index;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onDrop(targetIndex: number): void {
    if (this.draggedImageIndex === null || this.draggedImageIndex === targetIndex) return;

    this.images.update((list) => {
      const copy = [...list];
      const [dragged] = copy.splice(this.draggedImageIndex!, 1);
      copy.splice(targetIndex, 0, dragged);
      return copy.map((img, idx) => ({
        ...img,
        request: { ...img.request, sortOrder: idx },
      }));
    });
    this.draggedImageIndex = null;
  }

  // ── Save Flow ─────────────────────────────────────────────────
  saveProduct(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.saveMessage.set({
        type: 'error',
        text: 'Vui lòng điền đầy đủ các thông tin bắt buộc.',
      });
      return;
    }

    this.saving.set(true);
    this.saveMessage.set(null);

    const raw = this.form.getRawValue();
    const productReq: ProductRequest = {
      name: raw.name.trim(),
      categoryId: Number(raw.categoryId),
      brandId: raw.brandId ? Number(raw.brandId) : null,
      sku: raw.sku?.trim() || null,
      shortDesc: raw.shortDesc?.trim() || null,
      description: raw.description?.trim() || null,
      warrantyMonths: raw.warrantyMonths != null ? Number(raw.warrantyMonths) : 0,
      status: raw.status,
    };

    const saveProduct$ = this.isEditMode()
      ? this.productService.update(this.productId()!, productReq)
      : this.productService.create(productReq);

    saveProduct$.subscribe({
      next: (prodRes) => {
        const targetProdId = prodRes.data.productId;
        this.productId.set(targetProdId);
        this.isEditMode.set(true);

        this.saveVariantsAndImages(targetProdId);
      },
      error: (err) => {
        this.saving.set(false);
        this.saveMessage.set({
          type: 'error',
          text: err.error?.message || 'Lưu thông tin sản phẩm thất bại.',
        });
      },
    });
  }

  private saveVariantsAndImages(productId: number): void {
    const variantList = this.variants();
    const imageList = this.images();

    this.variants.update((list) =>
      list.map((v) => (v.saveStatus === 'saved' ? v : { ...v, saveStatus: 'saving' }))
    );
    this.images.update((list) =>
      list.map((img) => (img.saveStatus === 'saved' ? img : { ...img, saveStatus: 'saving' }))
    );

    const variantObs = variantList.map((v, i) => {
      if (v.saveStatus === 'saved') return of(v);

      const req$ = v.variantId
        ? this.productService.updateVariant(v.variantId, v.request)
        : this.productService.createVariant(productId, v.request);

      return req$.pipe(
        tap((res) => {
          this.updateVariantStatus(i, 'saved', res.data.variantId);
        }),
        catchError((err) => {
          const msg = err.error?.message || 'Lỗi lưu phiên bản';
          this.updateVariantStatus(i, 'error', null, msg);
          return of(null);
        })
      );
    });

    const imageObs = imageList.map((img, i) => {
      if (img.saveStatus === 'saved') return of(img);

      const req$ = img.imageId
        ? this.productService.updateImage(img.imageId, img.request)
        : this.productService.addImage(productId, img.request);

      return req$.pipe(
        tap((res) => {
          this.updateImageStatus(i, 'saved', res.data.imageId);
        }),
        catchError((err) => {
          const msg = err.error?.message || 'Lỗi lưu hình ảnh';
          this.updateImageStatus(i, 'error', null, msg);
          return of(null);
        })
      );
    });

    const specRequests: ProductAttributeValueRequest[] = [];
    const currentVals = this.attributeValues();
    for (const attr of this.availableAttributes()) {
      const val = currentVals[attr.attributeId];
      if (val != null && val.trim().length > 0) {
        specRequests.push({
          attributeId: attr.attributeId,
          value: val.trim()
        });
      }
    }
    const specs$ = this.attributeService.saveProductAttributes(productId, { attributes: specRequests }).pipe(
      catchError(() => of(null))
    );

    forkJoin({
      variants: forkJoin(variantObs.length ? variantObs : [of(null)]),
      images: forkJoin(imageObs.length ? imageObs : [of(null)]),
      specs: specs$,
    }).subscribe({
      next: () => {
        const savedImageIds = this.images()
          .map((img) => img.imageId)
          .filter((id): id is number => id != null);

        if (savedImageIds.length > 0) {
          this.productService.reorderImages(productId, savedImageIds).subscribe();
        }

        this.saving.set(false);
        this.evaluateSaveResult();
      },
      error: () => {
        this.saving.set(false);
        this.evaluateSaveResult();
      },
    });
  }

  private updateVariantStatus(
    index: number,
    status: 'saved' | 'error',
    newId: number | null = null,
    errorMessage: string | null = null
  ): void {
    this.variants.update((list) => {
      const copy = [...list];
      if (copy[index]) {
        copy[index] = {
          ...copy[index],
          saveStatus: status,
          variantId: newId ?? copy[index].variantId,
          errorMessage,
        };
      }
      return copy;
    });
  }

  private updateImageStatus(
    index: number,
    status: 'saved' | 'error',
    newId: number | null = null,
    errorMessage: string | null = null
  ): void {
    this.images.update((list) => {
      const copy = [...list];
      if (copy[index]) {
        copy[index] = {
          ...copy[index],
          saveStatus: status,
          imageId: newId ?? copy[index].imageId,
          errorMessage,
        };
      }
      return copy;
    });
  }

  private evaluateSaveResult(): void {
    const errorVariants = this.variants().filter((v) => v.saveStatus === 'error');
    const errorImages = this.images().filter((img) => img.saveStatus === 'error');

    if (errorVariants.length === 0 && errorImages.length === 0) {
      this.saveMessage.set({
        type: 'success',
        text: 'Đã lưu thành công sản phẩm và toàn bộ phiên bản, hình ảnh!',
      });
    } else {
      const parts = [];
      if (errorVariants.length > 0) {
        parts.push(`${errorVariants.length} phiên bản lỗi`);
      }
      if (errorImages.length > 0) {
        parts.push(`${errorImages.length} ảnh lỗi`);
      }
      this.saveMessage.set({
        type: 'warning',
        text: `Đã lưu sản phẩm nhưng có ${parts.join(' và ')}. Vui lòng kiểm tra lại.`,
      });
    }
  }

  hasError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }
}
