import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginationComponent {
  readonly currentPage = input<number>(0);
  readonly totalPages = input<number>(0);
  readonly totalElements = input<number>(0);
  readonly pageSize = input<number>(10);
  readonly itemLabel = input<string>('mục');

  readonly pageChanged = output<number>();

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i)
  );

  readonly paginationText = computed(() => {
    const total = this.totalElements();
    if (total === 0) return `0 - 0 trong 0 ${this.itemLabel()}`;
    const start = this.currentPage() * this.pageSize() + 1;
    const end = Math.min((this.currentPage() + 1) * this.pageSize(), total);
    return `${start} - ${end} trong tổng số ${total} ${this.itemLabel()}`;
  });

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) return;
    this.pageChanged.emit(page);
  }
}
