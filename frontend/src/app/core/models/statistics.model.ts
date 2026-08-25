export interface DashboardOverview {
  totalRevenue: number;
  revenueGrowthPercent: number;
  totalOrders: number;
  ordersGrowthPercent: number;
  averageOrderValue: number;
  newCustomers: number;
  customerGrowthPercent: number;
  lowStockCount: number;
}

export interface RevenueChartDataPoint {
  dateLabel: string;
  revenue: number;
  orderCount: number;
}

export interface TopSellingProduct {
  productId: number;
  productName: string;
  productSlug: string;
  categoryName: string;
  thumbnailImage: string | null;
  totalQuantitySold: number;
  totalRevenueGenerated: number;
}

export interface CategoryRevenueShare {
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  revenue: number;
  orderCount: number;
  percentageShare: number;
}

export type PeriodFilter = 'today' | '7days' | '30days' | 'month' | 'year' | 'custom';
