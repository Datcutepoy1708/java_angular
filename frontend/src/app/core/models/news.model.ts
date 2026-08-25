export type NewsStatus = 'draft' | 'published' | 'hidden';

export interface NewsCategory {
  newsCatId: number;
  name: string;
  slug: string;
  description?: string;
  sortOrder: number;
  status: 'active' | 'inactive';
}

export interface NewsCategoryRequest {
  name: string;
  slug?: string;
  description?: string;
  sortOrder?: number;
  status?: string;
}

export interface News {
  newsId: number;
  newsCatId?: number;
  categoryName?: string;
  categorySlug?: string;
  title: string;
  slug: string;
  thumbnailUrl?: string;
  summary?: string;
  content: string;
  authorId?: number;
  authorName?: string;
  viewCount: number;
  status: NewsStatus;
  publishedAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateNewsRequest {
  newsCatId?: number;
  title: string;
  slug?: string;
  thumbnailUrl?: string;
  summary?: string;
  content: string;
  status?: NewsStatus;
}

export interface UpdateNewsRequest {
  newsCatId?: number;
  title: string;
  slug?: string;
  thumbnailUrl?: string;
  summary?: string;
  content: string;
  status: NewsStatus;
}

export interface NewsFilterParams {
  categoryId?: number;
  status?: NewsStatus;
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}
