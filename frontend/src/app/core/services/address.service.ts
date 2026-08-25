import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { Address, AddressRequest } from '../models/address.model';

@Injectable({
  providedIn: 'root'
})
export class AddressService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/addresses`;

  getMyAddresses(): Observable<ApiResponse<Address[]>> {
    return this.http.get<ApiResponse<Address[]>>(this.baseUrl);
  }

  getAddressById(addressId: number): Observable<ApiResponse<Address>> {
    return this.http.get<ApiResponse<Address>>(`${this.baseUrl}/${addressId}`);
  }

  createAddress(request: AddressRequest): Observable<ApiResponse<Address>> {
    return this.http.post<ApiResponse<Address>>(this.baseUrl, request);
  }

  updateAddress(addressId: number, request: AddressRequest): Observable<ApiResponse<Address>> {
    return this.http.put<ApiResponse<Address>>(`${this.baseUrl}/${addressId}`, request);
  }

  deleteAddress(addressId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${addressId}`);
  }

  setDefaultAddress(addressId: number): Observable<ApiResponse<Address>> {
    return this.http.put<ApiResponse<Address>>(`${this.baseUrl}/${addressId}/default`, {});
  }
}
