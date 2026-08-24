package com.store.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest implements Serializable {

    private Integer categoryId;
    @Builder.Default
    private Boolean includeChildren = true;

    private Integer brandId;
    private Integer supplierId;
    private String status;
    private String keyword;
    private String attributes;

    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDir = "desc";

    public java.util.Map<Integer, java.util.List<String>> getParsedAttributeFilters() {
        if (attributes == null || attributes.isBlank()) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<Integer, java.util.List<String>> result = new java.util.HashMap<>();
        String[] attrGroups = attributes.split(";");
        for (String group : attrGroups) {
            String trimmedGroup = group.trim();
            if (trimmedGroup.isEmpty()) continue;

            int colonIndex = trimmedGroup.indexOf(':');
            if (colonIndex <= 0 || colonIndex == trimmedGroup.length() - 1) {
                throw new IllegalArgumentException("Định dạng tham số thuộc tính không hợp lệ: '" + trimmedGroup + "'. Định dạng mong đợi: 'attrId:val1,val2'");
            }
            try {
                Integer attrId = Integer.parseInt(trimmedGroup.substring(0, colonIndex).trim());
                String valuesPart = trimmedGroup.substring(colonIndex + 1).trim();
                String[] rawValues = valuesPart.split(",");
                java.util.List<String> values = new java.util.ArrayList<>();
                for (String v : rawValues) {
                    String cleanV = v.trim();
                    if (!cleanV.isEmpty()) {
                        values.add(cleanV);
                    }
                }
                if (!values.isEmpty()) {
                    result.computeIfAbsent(attrId, k -> new java.util.ArrayList<>()).addAll(values);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Mã thuộc tính không hợp lệ trong chuỗi lọc: '" + trimmedGroup + "'");
            }
        }
        return result;
    }
}
