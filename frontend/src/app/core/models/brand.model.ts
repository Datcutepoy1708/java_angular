export interface BrandResponse {
  brandId: number;
  name: string;
  slug: string;
  logoUrl: string | null;
  country: string | null;
  description: string | null;
  status: 'active' | 'inactive';
  deleted: boolean;
  deletedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/**
 * Slug is NOT sent on create — backend generates it from name via SlugUtil.
 * On edit: slug is shown read-only; only included if user explicitly unlocks it.
 */
export interface BrandRequest {
  name: string;
  logoUrl?: string | null;
  country?: string | null;
  description?: string | null;
  slug?: string | null;
  status?: 'active' | 'inactive';
}

export interface BrandPage {
  content: BrandResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
