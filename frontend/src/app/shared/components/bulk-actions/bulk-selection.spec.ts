import { describe, expect, it, vi } from 'vitest';
import { of, Subject, throwError } from 'rxjs';
import { BulkSelection } from './bulk-selection';

describe('BulkSelection', () => {
  it('selects only visible IDs and resets when the page changes', () => {
    const bulk = new BulkSelection();
    bulk.toggleAll([1, 2, 2]);
    expect(bulk.ids()).toEqual([1, 2]);
    bulk.toggle(1);
    expect(bulk.all([1, 2])).toBe(false);
    bulk.clear();
    expect(bulk.count()).toBe(0);
  });

  it('continues after HTTP and application failures and keeps failed IDs', async () => {
    const bulk = new BulkSelection();
    bulk.toggleAll([1, 2, 3, 4]);
    const run = vi.fn((id: number) => id === 2
      ? throwError(() => ({ error: { message: 'Không đủ quyền' } }))
      : of({ success: id !== 3, message: 'Trạng thái không hợp lệ' }));
    await bulk.execute({ label: 'Cập nhật', run }, '');
    expect(run.mock.calls.map(call => call[0])).toEqual([1, 2, 3, 4]);
    expect(bulk.ids()).toEqual([2, 3]);
    expect(bulk.failures()).toEqual([
      { id: 2, message: 'Không đủ quyền' },
      { id: 3, message: 'Trạng thái không hợp lệ' }
    ]);
    expect(bulk.report()).toContain('2 thành công, 2 thất bại');
    expect(bulk.busy()).toBe(false);
  });

  it('prevents duplicate submissions and selection changes while processing', async () => {
    const bulk = new BulkSelection();
    bulk.toggle(1);
    const response = new Subject<{ success: boolean }>();
    const run = vi.fn(() => response);
    const operation = { label: 'Cập nhật', run };
    const pending = bulk.execute(operation, '');
    await bulk.execute(operation, '');
    bulk.toggle(2);
    bulk.clear();
    bulk.toggleAll([3]);
    expect(bulk.ids()).toEqual([1]);
    expect(run).toHaveBeenCalledTimes(1);
    response.next({ success: true });
    await pending;
    expect(bulk.count()).toBe(0);
  });

  it('requires a reason before a destructive status transition', async () => {
    const bulk = new BulkSelection();
    bulk.toggle(1);
    const run = vi.fn(() => of({ success: true }));
    const operation = { label: 'Từ chối', run, requiresNote: true };
    await bulk.execute(operation, '  ');
    expect(run).not.toHaveBeenCalled();
    await bulk.execute(operation, '  Không hợp lệ  ');
    expect(run).toHaveBeenCalledWith(1, 'Không hợp lệ');
  });
});
