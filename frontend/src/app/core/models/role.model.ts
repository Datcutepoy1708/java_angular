export interface PermissionItem {
  permissionId: number;
  permissionCode: string;
  description: string;
  moduleGroup: string;
}

export interface PermissionGroup {
  groupCode: string;
  groupName: string;
  permissions: PermissionItem[];
}

export interface RoleDetail {
  roleId: number;
  roleName: string;
  description: string | null;
  isSystemRole: boolean;
  userCount: number;
  permissionCodes: string[];
  createdAt: string | null;
}

export interface RoleCreateRequest {
  roleName: string;
  description?: string;
  permissionCodes?: string[];
}

export interface RoleUpdateRequest {
  roleName?: string;
  description?: string;
}

export interface RolePermissionsUpdateRequest {
  permissionCodes: string[];
}
