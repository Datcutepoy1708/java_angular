// ─── Product Image ───────────────────────────────────────────────
export type ImageType = 'MAIN' | 'GALLERY';

export interface ProductImageResponse {
  imageId: number;
  productId: number | null;
  variantId: number | null;
  imageUrl: string;
  imageType: ImageType;
  altText: string | null;
  sortOrder: number;
  deleted?: boolean;
  deletedAt?: string | null;
}

export interface ProductImageRequest {
  imageUrl: string;
  imageType: ImageType;
  variantId?: number | null;
  altText?: string | null;
  sortOrder?: number;
}

// ─── Product Variant ─────────────────────────────────────────────
export type VariantStatus = 'active' | 'inactive';

export interface ProductVariantResponse {
  variantId: number;
  productId: number;
  productName: string;
  variantName: string;
  skuVariant: string | null;
  price: number;
  salePrice: number | null;
  costPrice: number | null;
  status: VariantStatus;
  mainImageUrl: string | null;
  images: ProductImageResponse[];
  deleted?: boolean;
  deletedAt?: string | null;
  createdAt: string | null;
}

export interface ProductVariantRequest {
  variantName: string;
  skuVariant?: string | null;
  price: number;
  salePrice?: number | null;
  costPrice?: number | null;
  status: VariantStatus;
}

// ─── Product ─────────────────────────────────────────────────────
export type ProductStatus = 'active' | 'inactive' | 'discontinued';

export interface ProductResponse {
  productId: number;
  categoryId: number | null;
  categoryName: string | null;
  categorySlug: string | null;
  brandId: number | null;
  brandName: string | null;
  brandSlug: string | null;
  supplierId: number | null;
  supplierName: string | null;
  name: string;
  slug: string;
  sku: string | null;
  shortDesc: string | null;
  description: string | null;
  warrantyMonths: number | null;
  status: ProductStatus;
  viewCount: number;
  mainImageUrl: string | null;
  images: ProductImageResponse[];
  variants: ProductVariantResponse[];
  deleted?: boolean;
  deletedAt?: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ProductRequest {
  categoryId: number;
  brandId?: number | null;
  supplierId?: number | null;
  name: string;
  slug?: string | null;
  sku?: string | null;
  shortDesc?: string | null;
  description?: string | null;
  warrantyMonths?: number | null;
  status: ProductStatus;
}

export interface ProductFilterRequest {
  categoryId?: number | null;
  brandId?: number | null;
  supplierId?: number | null;
  status?: string | null;
  keyword?: string | null;
  page?: number;
  size?: number;
}

// ─── Per-item save status for multi-step Product form ─────────────
export type ItemSaveStatus = 'pending' | 'saving' | 'saved' | 'error';

export interface VariantFormItem {
  variantId?: number | null; // null = not yet saved to backend
  request: ProductVariantRequest;
  saveStatus: ItemSaveStatus;
  errorMessage?: string | null;
}

export interface ImageFormItem {
  imageId?: number | null;
  request: ProductImageRequest;
  saveStatus: ItemSaveStatus;
  errorMessage?: string | null;
}
