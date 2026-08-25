export type ReviewStatus = 'APPROVED' | 'PENDING' | 'HIDDEN';

export interface Review {
  reviewId: number;
  productId: number;
  productName?: string;
  userId: number;
  userFullName?: string;
  userEmail?: string;
  rating: number;
  title?: string;
  comment: string;
  isVerifiedPurchase: boolean;
  status: ReviewStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface RatingSummary {
  productId: number;
  averageRating: number;
  totalReviews: number;
  ratingCounts: Record<number, number>;
  starPercentages: Record<number, number>;
}

export interface CreateReviewRequest {
  rating: number;
  title?: string;
  comment: string;
}

export interface ReviewFilterParams {
  productId?: number;
  rating?: number;
  status?: ReviewStatus;
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}
