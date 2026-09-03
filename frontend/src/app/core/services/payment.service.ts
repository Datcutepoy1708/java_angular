import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { PaymentPollingResponse } from '../models/order.model';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/payments`;

  constructor(private readonly http: HttpClient) {}

  getPaymentStatus(token: string): Observable<ApiResponse<PaymentPollingResponse>> {
    return this.http.post<ApiResponse<PaymentPollingResponse>>(`${this.apiUrl}/status`, { token });
  }
}
