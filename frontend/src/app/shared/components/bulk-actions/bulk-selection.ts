import { computed, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';

export interface BulkOperation {
  label: string;
  run: (id: number, note: string) => Observable<{ success: boolean; message?: string }>;
  requiresNote?: boolean;
  /** Controls button color in the bulk action bar. Defaults to 'default' (neutral). */
  variant?: 'default' | 'danger' | 'warning' | 'success' | 'info';
}

/** Page-local selection. Each request uses the domain API and its authorization rules. */
export class BulkSelection {
  readonly ids = signal<number[]>([]);
  readonly busy = signal(false);
  readonly report = signal('');
  readonly failures = signal<{ id: number; message: string }[]>([]);
  readonly count = computed(() => this.ids().length);

  toggle(id: number): void {
    if (this.busy()) return;
    this.ids.update(ids => ids.includes(id) ? ids.filter(value => value !== id) : [...ids, id]);
  }

  all(ids: number[]): boolean {
    return ids.length > 0 && ids.every(id => this.ids().includes(id));
  }

  toggleAll(ids: number[]): void {
    if (!this.busy()) this.ids.set(this.all(ids) ? [] : [...new Set(ids)]);
  }

  clear(): void {
    if (!this.busy()) {
      this.ids.set([]);
      this.report.set('');
      this.failures.set([]);
    }
  }

  async execute(operation: BulkOperation, note: string): Promise<void> {
    if (this.busy() || !this.count() || (operation.requiresNote && !note.trim())) return;
    const ids = [...this.ids()];
    this.busy.set(true);
    this.failures.set([]);
    let succeeded = 0;
    try {
      for (const id of ids) {
        try {
          const response = await firstValueFrom(operation.run(id, note.trim()));
          if (!response.success) throw new Error(response.message || 'Thao tác không thành công');
          succeeded++;
          this.ids.update(current => current.filter(value => value !== id));
        } catch (error: unknown) {
          const detail = error as { error?: { message?: string }; message?: string };
          this.failures.update(items => [...items, { id, message: detail?.error?.message || detail?.message || 'Không thể xử lý bản ghi' }]);
        }
        this.report.set(`Đã xử lý ${succeeded + this.failures().length}/${ids.length}: ${succeeded} thành công, ${this.failures().length} thất bại.`);
      }
    } finally {
      this.busy.set(false);
    }
  }
}
