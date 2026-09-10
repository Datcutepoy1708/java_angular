import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { of } from 'rxjs';
import { BulkActionsComponent } from './bulk-actions.component';
import { BulkSelection } from './bulk-selection';

describe('BulkActionsComponent', () => {
  it('requires confirmation and a reason before executing and reports completion', async () => {
    const fixture = TestBed.createComponent(BulkActionsComponent);
    const bulk = new BulkSelection();
    bulk.toggleAll([10, 20]);
    const run = vi.fn(() => of({ success: true }));
    fixture.componentRef.setInput('selection', bulk);
    fixture.componentRef.setInput('operations', [{ label: 'Từ chối', run, requiresNote: true }]);
    const completed = vi.fn();
    fixture.componentInstance.completed.subscribe(completed);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector('button')!.click();
    fixture.detectChanges();
    expect(run).not.toHaveBeenCalled();
    const confirmation = element.querySelector('.confirmation')!;
    expect(confirmation.querySelector('button')!.disabled).toBe(true);
    const note = confirmation.querySelector('input')!;
    note.value = 'Không đủ điều kiện';
    note.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    confirmation.querySelector('button')!.click();
    await fixture.whenStable();
    expect(run).toHaveBeenCalledTimes(2);
    expect(run).toHaveBeenCalledWith(10, 'Không đủ điều kiện');
    expect(completed).toHaveBeenCalledTimes(1);
    expect(bulk.report()).toContain('2 thành công, 0 thất bại');
  });

  it('does not send requests while the list is loading', async () => {
    const fixture = TestBed.createComponent(BulkActionsComponent);
    const bulk = new BulkSelection();
    bulk.toggle(1);
    const run = vi.fn(() => of({ success: true }));
    const operation = { label: 'Ẩn', run };
    fixture.componentRef.setInput('selection', bulk);
    fixture.componentRef.setInput('operations', [operation]);
    fixture.componentRef.setInput('disabled', true);
    await fixture.componentInstance.confirm(operation);
    expect(run).not.toHaveBeenCalled();
  });
});
