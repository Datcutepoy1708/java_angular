export type BannerPosition = 'homepage_slider' | 'sidebar' | 'popup' | 'category_top' | 'flank_left' | 'flank_right' | 'flank_both';
export type BannerStatus = 'active' | 'inactive';

export interface Banner {
  bannerId: number;
  title?: string;
  imageUrl: string;
  linkUrl?: string;
  position: BannerPosition;
  sortOrder: number;
  startDate?: string;
  endDate?: string;
  status: BannerStatus;
  createdAt: string;
}

export interface BannerRequest {
  title?: string;
  imageUrl: string;
  linkUrl?: string;
  position: BannerPosition;
  sortOrder?: number;
  startDate?: string;
  endDate?: string;
  status: BannerStatus;
}
