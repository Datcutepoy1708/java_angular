import { computed, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export interface CompareItem {
  productId: number;
  name: string;
  slug: string;
  imageUrl: string;
  price: number;
  salePrice?: number | null;
  categoryId?: number | null;
  categoryName?: string | null;
  brandName?: string | null;
}

export interface CompareNotification {
  text: string;
  type: 'success' | 'warning' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class CompareService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly storageKey = 'complexus_compare_items';

  readonly items = signal<CompareItem[]>(this.loadFromStorage());
  readonly count = computed(() => this.items().length);
  readonly isFull = computed(() => this.items().length >= 4);
  readonly notification = signal<CompareNotification | null>(null);

  private timeoutHandle: any = null;

  isInCompare(productId: number): boolean {
    return this.items().some((item) => item.productId === productId);
  }

  toggleCompare(item: CompareItem): boolean {
    if (this.isInCompare(item.productId)) {
      this.removeFromCompare(item.productId);
      return false;
    } else {
      return this.addToCompare(item);
    }
  }

  addToCompare(item: CompareItem): boolean {
    if (this.isInCompare(item.productId)) {
      this.showNotification(`Sản phẩm "${item.name}" đã có trong danh sách so sánh.`, 'info');
      return false;
    }

    if (this.items().length >= 4) {
      this.showNotification('Chỉ có thể so sánh tối đa 4 sản phẩm cùng lúc. Vui lòng xóa bớt sản phẩm!', 'warning');
      return false;
    }

    const current = this.items();
    if (current.length > 0 && item.categoryId && current[0].categoryId && item.categoryId !== current[0].categoryId) {
      this.showNotification(`Lưu ý: Bạn đang so sánh 2 sản phẩm thuộc danh mục khác nhau (${item.categoryName || 'mới'} vs ${current[0].categoryName || 'hiện tại'}).`, 'warning');
    } else {
      this.showNotification(`Đã thêm "${item.name}" vào danh sách so sánh (${current.length + 1}/4).`, 'success');
    }

    const next = [...current, item];
    this.items.set(next);
    this.saveToStorage(next);
    return true;
  }

  removeFromCompare(productId: number): void {
    const next = this.items().filter((i) => i.productId !== productId);
    this.items.set(next);
    this.saveToStorage(next);
    this.showNotification('Đã xóa sản phẩm khỏi danh sách so sánh.', 'info');
  }

  clear(): void {
    this.items.set([]);
    this.saveToStorage([]);
    this.showNotification('Đã làm trống danh sách so sánh.', 'info');
  }

  private showNotification(text: string, type: 'success' | 'warning' | 'info'): void {
    if (this.timeoutHandle) {
      clearTimeout(this.timeoutHandle);
    }
    this.notification.set({ text, type });
    this.timeoutHandle = setTimeout(() => {
      this.notification.set(null);
    }, 3500);
  }

  private loadFromStorage(): CompareItem[] {
    if (!isPlatformBrowser(this.platformId)) return [];
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed.slice(0, 4) : [];
    } catch {
      return [];
    }
  }

  private saveToStorage(items: CompareItem[]): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(items));
    } catch (e) {
      console.warn('Cannot save compare items to localStorage', e);
    }
  }
}
