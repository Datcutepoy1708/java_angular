import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { UserService } from './user.service';
import { environment } from '../../../environments/environment';
import { UserProfile } from '../models/user.model';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/users`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        UserService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch my profile and store in signal', () => {
    const mockProfile: UserProfile = {
      userId: 1,
      fullName: 'Nguyễn Văn An',
      email: 'an@example.com',
      phone: '0987654321',
      avatarUrl: null,
      gender: 'MALE',
      birthDate: '1995-05-15',
      status: 'ACTIVE',
      emailVerified: true,
      provider: 'LOCAL',
      roles: ['ROLE_CUSTOMER'],
      createdAt: '2026-08-01T00:00:00'
    };

    service.getMyProfile().subscribe((res) => {
      expect(res.data.fullName).toBe('Nguyễn Văn An');
      expect(service.userProfile()?.email).toBe('an@example.com');
    });

    const req = httpMock.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockProfile });
  });

  it('should update profile and update signal', () => {
    const updatedProfile: UserProfile = {
      userId: 1,
      fullName: 'Nguyễn Văn Bình',
      email: 'an@example.com',
      phone: '0912345678',
      avatarUrl: 'https://example.com/avatar.jpg',
      gender: 'OTHER',
      birthDate: '1996-06-20',
      status: 'ACTIVE',
      emailVerified: true,
      provider: 'LOCAL',
      roles: ['ROLE_CUSTOMER'],
      createdAt: '2026-08-01T00:00:00'
    };

    service.updateMyProfile({ fullName: 'Nguyễn Văn Bình', phone: '0912345678' }).subscribe((res) => {
      expect(res.data.fullName).toBe('Nguyễn Văn Bình');
      expect(service.userProfile()?.fullName).toBe('Nguyễn Văn Bình');
    });

    const req = httpMock.expectOne(`${baseUrl}/me`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true, message: 'Updated', data: updatedProfile });
  });

  it('should send password change request', () => {
    service.changePassword({ oldPassword: 'Old', newPassword: 'New', confirmPassword: 'New' }).subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/me/password`);
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true, message: 'Password changed', data: null });
  });
});
