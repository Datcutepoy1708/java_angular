export type SettingGroup =
  | 'GENERAL'
  | 'FOOTER'
  | 'ORDER_SHIPPING'
  | 'SEO'
  | 'SYSTEM_NOTIFICATION';

export interface SettingItem {
  settingId: number;
  settingKey: string;
  settingValue: string;
  settingGroup: SettingGroup;
  description?: string;
  isPublic: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type GroupedSettings = Record<SettingGroup, SettingItem[]>;

export interface UpdateSettingsPayload {
  settings: Record<string, string>;
}

export interface PublicSettings {
  storeName: string;
  storeSlogan: string;
  storeHotline: string;
  storeEmail: string;
  storeAddress: string;
  storeWorkingHours: string;
  footerBrandTitle: string;
  footerDescription: string;
  footerHotline: string;
  footerEmail: string;
  footerAddress: string;
  footerCopyright: string;
  footerBusinessLicense: string;
  footerFacebookUrl: string;
  footerYoutubeUrl: string;
  footerTiktokUrl: string;
  freeShippingThreshold: number;
  defaultShippingFee: number;
  enableCod: boolean;
  enableBankTransfer: boolean;
  metaTitle: string;
  metaDescription: string;
  maintenanceMode: boolean;
}

export const DEFAULT_PUBLIC_SETTINGS: PublicSettings = {
  storeName: 'Complexus Computer & Technology',
  storeSlogan: 'Đỉnh cao công nghệ PC Gaming & Linh kiện chính hãng',
  storeHotline: '1800 6868',
  storeEmail: 'support@complexus.vn',
  storeAddress: 'Số 123 Đường Công Nghệ, Quận Cầu Giấy, Hà Nội',
  storeWorkingHours: '08:00 - 21:30 (Thứ 2 - Chủ Nhật)',
  footerBrandTitle: 'COMPLEXUS',
  footerDescription: 'Hệ thống bán lẻ máy tính, laptop gaming, linh kiện PC và phụ kiện công nghệ chính hãng hàng đầu Việt Nam.',
  footerHotline: '1800 6868 (Miễn phí cuộc gọi, 8:00 - 21:30)',
  footerEmail: 'support@complexus.vn',
  footerAddress: '123 Đường Công Nghệ, Quận Cầu Giấy, Hà Nội',
  footerCopyright: '© 2026 Complexus — E-commerce Platform for Computers & Components. All rights reserved.',
  footerBusinessLicense: 'GPKD số: 0109876543 do Sở KH & ĐT TP. Hà Nội cấp ngày 15/01/2020.',
  footerFacebookUrl: 'https://facebook.com/complexus.tech',
  footerYoutubeUrl: 'https://youtube.com/@complexus_tech',
  footerTiktokUrl: 'https://tiktok.com/@complexus_tech',
  freeShippingThreshold: 5000000,
  defaultShippingFee: 35000,
  enableCod: true,
  enableBankTransfer: true,
  metaTitle: 'Complexus - Siêu thị Máy tính, Laptop Gaming & Linh kiện PC Chính Hãng',
  metaDescription: 'Chuyên cung cấp máy tính để bàn, laptop gaming, card màn hình VGA RTX 40-series, CPU Intel Gen 14, AMD Ryzen chính hãng giá tốt nhất.',
  maintenanceMode: false
};
