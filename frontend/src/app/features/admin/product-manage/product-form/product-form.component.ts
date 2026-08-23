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
import {
  ImageFormItem,
  ImageType,
  ProductRequest,
  ProductResponse,
  ProductStatus,
  VariantFormItem,
  VariantStatus,
} from '../../../../core/models/product.model';
import { CategoryResponse } from '../../../../core/models/category.model';
import { BrandResponse } from '../../../../core/models/brand.model';

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
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  // ── Mode & IDs ────────────────────────────────────────────────
  readonly isEditMode = signal(false);
  readonly productId = signal<number | null>(null);

  // ── Dropdown Options ──────────────────────────────────────────
  readonly categories = signal<CategoryResponse[]>([]);
  readonly brands = signal<BrandResponse[]>([]);

  // ── State ─────────────────────────────────────────────────────
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly saveMessage = signal<{ type: 'success' | 'warning' | 'error'; text: string } | null>(null);

  // ── Variants & Images Lists ───────────────────────────────────
  readonly variants = signal<VariantFormItem[]>([]);
  readonly images = signal<ImageFormItem[]>([]);

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
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.productId.set(Number(idParam));
      this.loadProductData(Number(idParam));
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
      // If already persisted on server, call delete API
      this.productService.deleteVariant(item.variantId).subscribe({
        next: () => {
          this.variants.update((list) => list.filter((_, i) => i !== index));
        },
      });
    } else {
      this.variants.update((list) => list.filter((_, i) => i !== index));
    }
  }

  updateVariantField(index: number, field: string, value: any): void {
    this.variants.update((list) => {
      const copy = [...list];
      const target = { ...copy[index] };
      const req = { ...target.request, [field]: value };
      target.request = req;
      if (target.saveStatus === 'saved') {
        target.saveStatus = 'pending'; // Mark as dirty
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
      this.productService.deleteImage(item.imageId).subscribe({
        next: () => {
          this.images.update((list) => list.filter((_, i) => i !== index));
          this.ensureMainImage();
        },
      });
    } else {
      this.images.update((list) => list.filter((_, i) => i !== index));
      this.ensureMainImage();
    }
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
      // Update sortOrder
      return copy.map((img, idx) => ({
        ...img,
        request: { ...img.request, sortOrder: idx },
      }));
    });
    this.draggedImageIndex = null;
  }

  // ── Save Flow (Multi-step with partial failure handling) ───────
  saveProduct(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.saveMessage.set({
        type: 'error',
        text: 'Please fill in all required fields.',
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

    // Step 1: Save Product
    const saveProduct$ = this.isEditMode()
      ? this.productService.update(this.productId()!, productReq)
      : this.productService.create(productReq);

    saveProduct$.subscribe({
      next: (prodRes) => {
        const targetProdId = prodRes.data.productId;
        this.productId.set(targetProdId);
        this.isEditMode.set(true);

        // Step 2 & 3: Save Variants & Images in parallel with catchError per item
        this.saveVariantsAndImages(targetProdId);
      },
      error: (err) => {
        this.saving.set(false);
        this.saveMessage.set({
          type: 'error',
          text: err.error?.message || 'Failed to save product information.',
        });
      },
    });
  }

  private saveVariantsAndImages(productId: number): void {
    const variantList = this.variants();
    const imageList = this.images();

    // Mark items as saving
    this.variants.update((list) =>
      list.map((v) => (v.saveStatus === 'saved' ? v : { ...v, saveStatus: 'saving' }))
    );
    this.images.update((list) =>
      list.map((img) => (img.saveStatus === 'saved' ? img : { ...img, saveStatus: 'saving' }))
    );

    // Build Variant Observables
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
          const msg = err.error?.message || 'Error saving variant';
          this.updateVariantStatus(i, 'error', null, msg);
          return of(null); // Prevent forkJoin from cancelling
        })
      );
    });

    // Build Image Observables
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
          const msg = err.error?.message || 'Error saving image';
          this.updateImageStatus(i, 'error', null, msg);
          return of(null); // Prevent forkJoin from cancelling
        })
      );
    });

    forkJoin({
      variants: forkJoin(variantObs.length ? variantObs : [of(null)]),
      images: forkJoin(imageObs.length ? imageObs : [of(null)]),
    }).subscribe({
      next: () => {
        // Reorder images if all images have imageId
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
        text: 'Product and all nested items saved successfully!',
      });
    } else {
      const parts = [];
      if (errorVariants.length > 0) {
        parts.push(`${errorVariants.length} variant(s) failed`);
      }
      if (errorImages.length > 0) {
        parts.push(`${errorImages.length} image(s) failed`);
      }
      this.saveMessage.set({
        type: 'warning',
        text: `Product saved, but ${parts.join(' and ')}. Check the highlighted items and try saving again.`,
      });
    }
  }

  // Helper
  hasError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }
}
