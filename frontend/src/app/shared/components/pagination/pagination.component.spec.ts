import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginationComponent } from './pagination.component';

describe('PaginationComponent', () => {
  let component: PaginationComponent;
  let fixture: ComponentFixture<PaginationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginationComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render pagination text correctly', () => {
    fixture.componentRef.setInput('currentPage', 0);
    fixture.componentRef.setInput('pageSize', 10);
    fixture.componentRef.setInput('totalElements', 25);
    fixture.componentRef.setInput('totalPages', 3);
    fixture.componentRef.setInput('itemLabel', 'danh mục');
    fixture.detectChanges();

    const info = fixture.nativeElement.querySelector('.pagination-info');
    expect(info.textContent).toContain('1 - 10 trong tổng số 25 danh mục');
  });

  it('should emit pageChanged event when page button is clicked', () => {
    let selectedPage = -1;
    component.pageChanged.subscribe((page) => (selectedPage = page));

    fixture.componentRef.setInput('currentPage', 0);
    fixture.componentRef.setInput('pageSize', 10);
    fixture.componentRef.setInput('totalElements', 25);
    fixture.componentRef.setInput('totalPages', 3);
    fixture.detectChanges();

    const pageButtons = fixture.nativeElement.querySelectorAll('.page-num-btn');
    expect(pageButtons.length).toBe(3);

    pageButtons[1].click(); // click page 2 (index 1)
    expect(selectedPage).toBe(1);
  });
});
