export type AttributeDataType = 'text' | 'number' | 'boolean';

export interface AttributeResponse {
  attributeId: number;
  categoryId: number;
  categoryName?: string;
  name: string;
  dataType: AttributeDataType;
  unit?: string;
  sortOrder: number;
}

export interface AttributeRequest {
  categoryId: number;
  name: string;
  dataType: AttributeDataType;
  unit?: string;
  sortOrder?: number;
}

export interface ProductAttributeValueResponse {
  id?: number;
  attributeId: number;
  attributeName: string;
  dataType?: AttributeDataType;
  unit?: string;
  value: string;
}

export interface ProductAttributeValueRequest {
  attributeId: number;
  value: string;
}

export interface BatchSaveProductAttributesRequest {
  attributes: ProductAttributeValueRequest[];
}
