export interface Address {
  addressId: number;
  userId?: number;
  receiverName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  detailAddress: string;
  fullAddress?: string;
  isDefault: boolean;
}

export interface AddressRequest {
  receiverName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  detailAddress: string;
  isDefault?: boolean;
}
