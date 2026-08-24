import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import {
  AttributeRequest,
  AttributeResponse,
  BatchSaveProductAttributesRequest,
  ProductAttributeValueResponse
} from '../models/attribute.model';

@Injectable({
  providedIn: 'root'
})
export class AttributeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/attributes`;
  private readonly productAttributesUrl = `${environment.apiUrl}/api/v1/products`;

  getByCategory(categoryId: number): Observable<ApiResponse<AttributeResponse[]>> {
    return this.http.get<ApiResponse<AttributeResponse[]>>(`${this.baseUrl}/category/${categoryId}`);
  }

  getById(id: number): Observable<ApiResponse<AttributeResponse>> {
    return this.http.get<ApiResponse<AttributeResponse>>(`${this.baseUrl}/${id}`);
  }

  create(request: AttributeRequest): Observable<ApiResponse<AttributeResponse>> {
    return this.http.post<ApiResponse<AttributeResponse>>(this.baseUrl, request);
  }

  update(id: number, request: AttributeRequest): Observable<ApiResponse<AttributeResponse>> {
    return this.http.put<ApiResponse<AttributeResponse>>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${id}`);
  }

  getProductAttributes(productId: number): Observable<ApiResponse<ProductAttributeValueResponse[]>> {
    return this.http.get<ApiResponse<ProductAttributeValueResponse[]>>(
      `${this.productAttributesUrl}/${productId}/attributes`
    );
  }

  saveProductAttributes(
    productId: number,
    request: BatchSaveProductAttributesRequest
  ): Observable<ApiResponse<ProductAttributeValueResponse[]>> {
    return this.http.put<ApiResponse<ProductAttributeValueResponse[]>>(
      `${this.productAttributesUrl}/${productId}/attributes`,
      request
    );
  }

  deleteProductAttribute(productId: number, attributeId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.productAttributesUrl}/${productId}/attributes/${attributeId}`
    );
  }
}
