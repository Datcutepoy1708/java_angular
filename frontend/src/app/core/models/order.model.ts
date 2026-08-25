export type OrderStatus = 'pending' | 'confirmed' | 'processing' | 'shipping' | 'completed' | 'cancelled';
export type PaymentStatus = 'unpaid' | 'paid' | 'refunded';
export type PaymentMethod = 'cod' | 'bank_transfer' | 'vnpay' | 'momo' | 'zalopay';

export interface OrderItem {
  orderItemId: number;
  variantId: number;
  variantName: string;
  skuVariant: string;
  productId: number;
  productName: string;
  productSlug: string;
  imageUrl?: string;
  warehouseId?: number;
  warehouseName?: string;
  productNameSnapshot: string;
  priceSnapshot: number;
  quantity: number;
  subtotal: number;
}

export interface OrderStatusHistory {
  id: number;
  status: OrderStatus;
  note?: string;
  changedById?: number;
  changedByName?: string;
  changedAt: string;
}

export interface Order {
  orderId: number;
  orderCode: string;
  userId: number;
  userEmail?: string;
  userFullName?: string;
  addressId?: number;
  receiverName: string;
  receiverPhone: string;
  shippingAddress: string;
  subtotal: number;
  discountAmount: number;
  shippingFee: number;
  totalAmount: number;
  discountId?: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  orderStatus: OrderStatus;
  note?: string;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  statusHistory: OrderStatusHistory[];
}

export interface CreateOrderRequest {
  addressId?: number;
  receiverName?: string;
  receiverPhone?: string;
  shippingAddress?: string;
  province?: string;
  district?: string;
  ward?: string;
  detailAddress?: string;
  paymentMethod: PaymentMethod;
  note?: string;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
  note?: string;
}

export interface UpdatePaymentStatusRequest {
  paymentStatus: PaymentStatus;
  note?: string;
}

export interface OrderFilter {
  status?: OrderStatus | '';
  paymentStatus?: PaymentStatus | '';
  keyword?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface OrderMetrics {
  totalOrders: number;
  pendingCount: number;
  confirmedCount: number;
  processingCount: number;
  shippingCount: number;
  completedCount: number;
  cancelledCount: number;
  unpaidCount: number;
  paidCount: number;
  totalRevenue: number;
}
