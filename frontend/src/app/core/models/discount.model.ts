export type DiscountType = 'percent' | 'fixed';
export type DiscountStatus = 'active' | 'inactive' | 'expired';

export interface Discount {
  discountId: number;
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  maxDiscountAmount?: number;
  minOrderValue: number;
  usageLimit?: number;
  usageLimitPerUser: number;
  usedCount: number;
  applicableCategoryId?: number;
  applicableCategoryName?: string;
  startDate: string;
  endDate: string;
  status: DiscountStatus;
  createdAt: string;
  isValidNow?: boolean;
}

export interface DiscountValidationResult {
  valid: boolean;
  discountId: number;
  code: string;
  discountType: DiscountType;
  discountValue: number;
  discountAmount: number;
  subtotal: number;
  finalTotal: number;
  description?: string;
  message: string;
}

export interface DiscountMetrics {
  totalDiscounts: number;
  activeDiscounts: number;
  totalUsedCount: number;
  expiredDiscounts: number;
}

export interface DiscountUsage {
  id: number;
  discountId: number;
  discountCode: string;
  userId: number;
  userFullName: string;
  userEmail: string;
  orderId: number;
  orderCode: string;
  orderTotal: number;
  usedAt: string;
}

export interface DiscountFilterParams {
  keyword?: string;
  status?: DiscountStatus;
  discountType?: DiscountType;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
