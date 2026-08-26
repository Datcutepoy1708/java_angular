export type ReturnStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'ITEM_RECEIVED' | 'REFUNDED' | 'CANCELLED';

export type ReturnReason = 'DEFECTIVE' | 'WRONG_ITEM' | 'DAMAGED_IN_TRANSIT' | 'NOT_AS_DESCRIBED' | 'CHANGE_OF_MIND' | 'OTHER';

export type ItemCondition = 'NEW_SEAL' | 'OPENED' | 'DAMAGED' | 'DEFECTIVE';

export interface ReturnItemDetail {
  id: number;
  orderItemId: number;
  variantId?: number;
  productName: string;
  variantName?: string;
  skuVariant?: string;
  imageUrl?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  itemCondition: ItemCondition;
}

export interface ReturnDetail {
  returnId: number;
  returnCode: string;
  orderId: number;
  orderTrackingNumber: string;
  userId: number;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  status: ReturnStatus;
  returnReason: ReturnReason;
  customerNote?: string;
  adminNote?: string;
  refundAmount: number;
  bankName?: string;
  bankAccountNumber?: string;
  bankAccountName?: string;
  refundTransactionCode?: string;
  restockWarehouseId?: number;
  restockWarehouseName?: string;
  requestedAt: string;
  approvedAt?: string;
  receivedAt?: string;
  refundedAt?: string;
  createdAt: string;
  updatedAt: string;
  items: ReturnItemDetail[];
  imageUrls: string[];
}

export interface ReturnCreateItemRequest {
  orderItemId: number;
  quantity: number;
  itemCondition?: string;
}

export interface ReturnCreateRequest {
  orderId: number;
  returnReason: ReturnReason | string;
  customerNote?: string;
  items: ReturnCreateItemRequest[];
  imageUrls?: string[];
  bankName?: string;
  bankAccountNumber?: string;
  bankAccountName?: string;
}

export interface ReturnReviewRequest {
  approved: boolean;
  adminNote?: string;
}

export interface ReturnReceiveItemRequest {
  warehouseId: number;
  adminNote?: string;
  itemConditions?: {
    returnItemId: number;
    condition: ItemCondition | string;
  }[];
}

export interface ReturnProcessRefundRequest {
  refundTransactionCode: string;
  adminNote?: string;
}

export interface ReturnFilter {
  keyword?: string;
  status?: string;
  reason?: string;
  userId?: number;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface ReturnMetrics {
  totalRequests: number;
  pendingReviewCount: number;
  awaitingItemCount: number;
  refundedCount: number;
  rejectedCount: number;
  totalRefundedAmount: number;
}
