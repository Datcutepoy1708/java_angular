export interface CategoryResponse {
  categoryId: number;
  parentId: number | null;
  parentName: string | null;
  name: string;
  slug: string;
  iconUrl: string | null;
  description: string | null;
  sortOrder: number | null;
  status: 'active' | 'inactive' | string;
  deleted: boolean;
  deletedAt: string | null;
  children: CategoryResponse[];
}

/**
 * Slug is NOT sent on create — backend generates it from name.
 * On edit: slug shown read-only, only sent if user explicitly edits it.
 */
export interface CategoryRequest {
  name: string;
  parentId?: number | null;
  iconUrl?: string | null;
  description?: string | null;
  sortOrder?: number | null;
  status: 'active' | 'inactive';
  slug?: string | null; // optional — omit on create
}

export interface CategoryChildrenCount {
  categoryId: number;
  childrenCount: number;
}
