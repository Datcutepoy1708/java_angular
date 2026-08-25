import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ReviewService } from '../../../core/services/review.service';
import { Review, ReviewFilterParams, ReviewStatus } from '../../../core/models/review.model';
import { RatingStarsComponent } from '../../../shared/components/rating-stars/rating-stars.component';

@Component({
  selector: 'app-review-manage',
  standalone: true,
  imports: [CommonModule, DatePipe, RatingStarsComponent],
  templateUrl: './review-manage.component.html',
  styleUrl: './review-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewManageComponent implements OnInit {
  private readonly reviewService = inject(ReviewService);

  readonly reviews = signal<Review[]>([]);
  readonly loading = signal<boolean>(false);

  // Filters & Pagination
  readonly filterRating = signal<number | null>(null);
  readonly filterStatus = signal<ReviewStatus | ''>('');
  readonly filterKeyword = signal<string>('');
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(1);

  // Delete Confirm State
  readonly showDeleteModal = signal<boolean>(false);
  readonly deletingReviewId = signal<number | null>(null);
  readonly deletingCustomerName = signal<string>('');

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    this.loading.set(true);
    const filter: ReviewFilterParams = {
      rating: this.filterRating() ?? undefined,
      status: this.filterStatus() ? (this.filterStatus() as ReviewStatus) : undefined,
      keyword: this.filterKeyword().trim() || undefined,
      page: this.currentPage(),
      size: this.pageSize(),
      sortBy: 'createdAt',
      sortDir: 'desc',
    };

    this.reviewService.getAdminReviews(filter).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.reviews.set(res.data.content);
          this.totalPages.set(res.data.totalPages);
          this.totalElements.set(res.data.totalElements);
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading admin reviews:', err);
        this.loading.set(false);
      },
    });
  }

  onSearch(): void {
    this.currentPage.set(0);
    this.loadReviews();
  }

  resetFilter(): void {
    this.filterRating.set(null);
    this.filterStatus.set('');
    this.filterKeyword.set('');
    this.currentPage.set(0);
    this.loadReviews();
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadReviews();
    }
  }

  updateStatus(reviewId: number, status: ReviewStatus): void {
    this.reviewService.updateReviewStatus(reviewId, status).subscribe({
      next: () => this.loadReviews(),
      error: (err) => console.error('Error updating review status:', err),
    });
  }

  openDeleteModal(review: Review): void {
    this.deletingReviewId.set(review.reviewId);
    this.deletingCustomerName.set(review.userFullName || `Khách hàng #${review.userId}`);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deletingReviewId.set(null);
  }

  confirmDelete(): void {
    const id = this.deletingReviewId();
    if (!id) return;

    this.reviewService.deleteReview(id).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.loadReviews();
      },
      error: (err) => {
        console.error('Error deleting review:', err);
        this.closeDeleteModal();
      },
    });
  }
}
