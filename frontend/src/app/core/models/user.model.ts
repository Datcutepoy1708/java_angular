export type GenderType = 'MALE' | 'FEMALE' | 'OTHER';

export interface UserProfile {
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  avatarUrl: string | null;
  gender: GenderType | null;
  birthDate: string | null;
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED';
  emailVerified: boolean;
  provider: 'LOCAL' | 'GOOGLE' | 'FACEBOOK';
  roles: string[];
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateProfilePayload {
  fullName: string;
  phone?: string | null;
  gender?: GenderType | null;
  birthDate?: string | null;
  avatarUrl?: string | null;
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}
