import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageUploadComponent } from './image-upload.component';

describe('ImageUploadComponent', () => {
  let component: ImageUploadComponent;
  let fixture: ComponentFixture<ImageUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageUploadComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ImageUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should switch mode between upload and url', () => {
    expect(component.inputMode()).toBe('upload');

    component.setMode('url');
    expect(component.inputMode()).toBe('url');

    component.setMode('upload');
    expect(component.inputMode()).toBe('upload');
  });

  it('should emit imageRemoved when remove button clicked', () => {
    let removed = false;
    component.imageRemoved.subscribe(() => (removed = true));

    fixture.componentRef.setInput('imageUrl', 'https://example.com/logo.png');
    fixture.detectChanges();

    const removeBtn = fixture.nativeElement.querySelector('.btn-remove');
    removeBtn?.click();

    expect(removed).toBe(true);
  });
});
