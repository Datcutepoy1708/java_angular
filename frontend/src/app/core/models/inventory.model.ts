export interface Warehouse {
  warehouseId: number;
  name: string;
  address?: string;
  phone?: string;
}

export interface WarehouseStock {
  warehouseId: number;
  warehouseName: string;
  warehouseAddress?: string;
  quantity: number;
  reservedQty: number;
  availableQty: number;
}

export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';

export interface InventoryItem {
  inventoryId: number;
  variantId: number;
  variantName: string;
  skuVariant: string;
  price?: number;
  salePrice?: number;
  productId: number;
  productName: string;
  productSlug: string;
  productThumbnail?: string;
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  reservedQty: number;
  availableQty: number;
  stockStatus: StockStatus;
  updatedAt: string;
}

export interface VariantStockSummary {
  variantId: number;
  variantName: string;
  skuVariant: string;
  productId: number;
  productName: string;
  totalQuantity: number;
  totalReservedQty: number;
  totalAvailableQty: number;
  inStock: boolean;
  stockStatus: StockStatus;
  warehouseBreakdown: WarehouseStock[];
}

export type InventoryChangeType = 'import' | 'sale' | 'return' | 'adjust' | 'transfer';

export interface InventoryLog {
  logId: number;
  variantId: number;
  variantName: string;
  skuVariant: string;
  productId: number;
  productName: string;
  warehouseId: number;
  warehouseName: string;
  changeType: InventoryChangeType;
  quantityChange: number;
  referenceType?: string;
  referenceId?: number;
  note?: string;
  createdByUserId?: number;
  createdByUserName?: string;
  createdAt: string;
}

export interface InventoryMetrics {
  totalTrackedItems: number;
  lowStockItemsCount: number;
  outOfStockItemsCount: number;
  totalPhysicalQuantity: number;
}

export interface StockAdjustmentRequest {
  variantId: number;
  warehouseId: number;
  quantityChange: number;
  reason: string;
}

export interface StockImportItemRequest {
  variantId: number;
  quantity: number;
  costPrice?: number;
}

export interface StockImportRequest {
  warehouseId: number;
  supplierId?: number;
  note?: string;
  items: StockImportItemRequest[];
}

export interface StockTransferRequest {
  fromWarehouseId: number;
  toWarehouseId: number;
  variantId: number;
  quantity: number;
  note?: string;
}

export interface InventoryFilterParams {
  warehouseId?: number;
  keyword?: string;
  stockStatus?: 'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface InventoryLogFilterParams {
  variantId?: number;
  warehouseId?: number;
  keyword?: string;
  page?: number;
  size?: number;
}
