import { TestBed } from '@angular/core/testing';
import { CompareItem, CompareService } from './compare.service';

describe('CompareService', () => {
  let service: CompareService;

  const mockItem1: CompareItem = {
    productId: 1,
    name: 'Laptop Gaming ASUS ROG Strix G16',
    slug: 'laptop-gaming-asus-rog-strix-g16',
    imageUrl: '/uploads/asus-g16.png',
    price: 32000000,
    salePrice: 29990000,
    categoryId: 1,
    categoryName: 'Laptop Gaming',
    brandName: 'ASUS',
  };

  const mockItem2: CompareItem = {
    productId: 2,
    name: 'Laptop Gaming Acer Predator Helios 16',
    slug: 'laptop-predator-helios-16',
    imageUrl: '/uploads/helios-16.png',
    price: 35000000,
    salePrice: 33500000,
    categoryId: 1,
    categoryName: 'Laptop Gaming',
    brandName: 'Acer',
  };

  const mockItem3: CompareItem = {
    productId: 3,
    name: 'Laptop Gaming Lenovo Legion 5 Pro',
    slug: 'laptop-legion-5-pro',
    imageUrl: '/uploads/legion-5.png',
    price: 38000000,
    salePrice: null,
    categoryId: 1,
    categoryName: 'Laptop Gaming',
    brandName: 'Lenovo',
  };

  const mockItem4: CompareItem = {
    productId: 4,
    name: 'Laptop Gaming MSI Katana 15',
    slug: 'laptop-msi-katana-15',
    imageUrl: '/uploads/msi-katana.png',
    price: 25000000,
    salePrice: 23990000,
    categoryId: 1,
    categoryName: 'Laptop Gaming',
    brandName: 'MSI',
  };

  const mockItem5: CompareItem = {
    productId: 5,
    name: 'Laptop Dell Alienware m16',
    slug: 'laptop-alienware-m16',
    imageUrl: '/uploads/alienware.png',
    price: 55000000,
    salePrice: null,
    categoryId: 1,
    categoryName: 'Laptop Gaming',
    brandName: 'Dell',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [CompareService],
    });
    service = TestBed.inject(CompareService);
    service.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created and start empty', () => {
    expect(service).toBeTruthy();
    expect(service.items().length).toBe(0);
    expect(service.count()).toBe(0);
    expect(service.isFull()).toBe(false);
  });

  it('should add items up to 4 products', () => {
    expect(service.addToCompare(mockItem1)).toBe(true);
    expect(service.addToCompare(mockItem2)).toBe(true);
    expect(service.addToCompare(mockItem3)).toBe(true);
    expect(service.addToCompare(mockItem4)).toBe(true);

    expect(service.count()).toBe(4);
    expect(service.isFull()).toBe(true);

    // 5th item must be rejected
    const added5 = service.addToCompare(mockItem5);
    expect(added5).toBe(false);
    expect(service.count()).toBe(4);
    expect(service.notification()?.type).toBe('warning');
  });

  it('should not allow duplicate products', () => {
    service.addToCompare(mockItem1);
    const addedDuplicate = service.addToCompare(mockItem1);
    expect(addedDuplicate).toBe(false);
    expect(service.count()).toBe(1);
  });

  it('should toggle compare status correctly', () => {
    // 1st toggle: adds item
    const toggledOn = service.toggleCompare(mockItem1);
    expect(toggledOn).toBe(true);
    expect(service.isInCompare(mockItem1.productId)).toBe(true);

    // 2nd toggle: removes item
    const toggledOff = service.toggleCompare(mockItem1);
    expect(toggledOff).toBe(false);
    expect(service.isInCompare(mockItem1.productId)).toBe(false);
  });

  it('should remove item and clear all items', () => {
    service.addToCompare(mockItem1);
    service.addToCompare(mockItem2);
    expect(service.count()).toBe(2);

    service.removeFromCompare(mockItem1.productId);
    expect(service.count()).toBe(1);
    expect(service.items()[0].productId).toBe(mockItem2.productId);

    service.clear();
    expect(service.count()).toBe(0);
    expect(service.items().length).toBe(0);
  });
});
