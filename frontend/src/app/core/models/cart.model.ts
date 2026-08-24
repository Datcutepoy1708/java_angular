export interface CartItem {
  cartId?: number | null;
  variantId: number;
  variantName: string;
  skuVariant: string;
  price: number;
  originalPrice: number;
  imageUrl?: string | null;
  productId: number;
  productName: string;
  productSlug: string;
  quantity: number;
  subtotal: number;
  availableQty: number;
  isAvailable: boolean;
  isExceededStock: boolean;
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  totalQuantity: number;
  totalAmount: number;
  originalTotalAmount: number;
  savingsAmount: number;
  removedStaleItemsCount?: number;
}

export interface AddToCartRequest {
  variantId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface CartItemSyncDto {
  variantId: number;
  quantity: number;
}

export interface MergeCartRequest {
  items: CartItemSyncDto[];
}

export interface LocalCartItem {
  variantId: number;
  quantity: number;
  addedAt: number;
}
