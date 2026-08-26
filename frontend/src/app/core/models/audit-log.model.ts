export interface AuditLogItem {
  logId: number;
  userId?: number;
  userEmail?: string;
  actionType: string;
  module: string;
  recordId?: string;
  description: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
  userAgent?: string;
  status: string;
  createdAt: string;
}

export interface AuditLogFilter {
  keyword?: string;
  module?: string;
  actionType?: string;
  userId?: number;
  status?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface AuditLogPageResponse {
  content: AuditLogItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
