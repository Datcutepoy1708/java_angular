export interface Role {
  roleId: number;
  roleName: string;
  description: string;
}

export interface AdminUser {
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  gender: 'male' | 'female' | 'other' | null;
  birthDate: string | null;
  status: 'active' | 'inactive' | 'banned';
  emailVerified: boolean;
  provider: string;
  roles: string[];
  createdAt: string | null;
  updatedAt: string | null;
  totalOrders: number;
  totalSpend: number;
}

export interface AdminUserCreateRequest {
  fullName: string;
  email: string;
  phone?: string | null;
  password: string;
  gender?: string | null;
  birthDate?: string | null;
  roles: string[];
  status?: string;
}

export interface AdminUserUpdateRequest {
  fullName: string;
  phone?: string | null;
  gender?: string | null;
  birthDate?: string | null;
  roles: string[];
  status: string;
}

export interface AdminUserStatusRequest {
  status: string;
}

export interface AdminUserPasswordResetRequest {
  newPassword: string;
}

export interface AdminUserPage {
  content: AdminUser[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
