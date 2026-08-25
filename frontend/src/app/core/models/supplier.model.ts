export interface SupplierResponse {
  supplierId: number;
  name: string;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  status: 'active' | 'inactive';
  createdAt: string | null;
  productCount: number;
}

export interface SupplierRequest {
  name: string;
  contactName?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  status?: 'active' | 'inactive';
}

export interface SupplierPage {
  content: SupplierResponse[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
