package com.store.repository;

import com.store.entity.product.Product;
import com.store.entity.product.ProductAttributeValue;
import com.store.entity.product.ProductStatus;
import com.store.entity.product.ProductVariant;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductSpecification {

    public static Specification<Product> filterBy(
            Integer categoryId,
            List<Integer> categoryIds,
            Integer brandId,
            Integer supplierId,
            ProductStatus status,
            String keyword,
            Map<Integer, List<String>> attributeFilters,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("categoryId").in(categoryIds));
            } else if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("categoryId"), categoryId));
            }

            if (brandId != null) {
                predicates.add(criteriaBuilder.equal(root.get("brand").get("brandId"), brandId));
            }

            if (supplierId != null) {
                predicates.add(criteriaBuilder.equal(root.get("supplier").get("supplierId"), supplierId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
                Predicate skuLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), pattern);
                Predicate slugLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), pattern);
                predicates.add(criteriaBuilder.or(nameLike, skuLike, slugLike));
            }

            // Dynamic Correlated EXISTS Subqueries for EAV Attributes (No N+1, No duplicate rows)
            if (attributeFilters != null && !attributeFilters.isEmpty()) {
                for (Map.Entry<Integer, List<String>> entry : attributeFilters.entrySet()) {
                    Integer attrId = entry.getKey();
                    List<String> values = entry.getValue();
                    if (attrId != null && values != null && !values.isEmpty()) {
                        Subquery<Long> subquery = query.subquery(Long.class);
                        Root<ProductAttributeValue> pavRoot = subquery.from(ProductAttributeValue.class);
                        subquery.select(pavRoot.get("id"))
                                .where(
                                        criteriaBuilder.equal(pavRoot.get("product"), root),
                                        criteriaBuilder.equal(pavRoot.get("attribute").get("attributeId"), attrId),
                                        pavRoot.get("value").in(values)
                                );
                        predicates.add(criteriaBuilder.exists(subquery));
                    }
                }
            }

            // Price range filter via ProductVariant subquery
            if (minPrice != null || maxPrice != null) {
                Subquery<Long> variantSubquery = query.subquery(Long.class);
                Root<ProductVariant> variantRoot = variantSubquery.from(ProductVariant.class);
                List<Predicate> variantPreds = new ArrayList<>();
                variantPreds.add(criteriaBuilder.equal(variantRoot.get("product"), root));
                variantPreds.add(criteriaBuilder.isNull(variantRoot.get("deletedAt")));

                Expression<BigDecimal> effectivePrice = criteriaBuilder.coalesce(
                        variantRoot.get("salePrice"),
                        variantRoot.get("price")
                );

                if (minPrice != null) {
                    variantPreds.add(criteriaBuilder.greaterThanOrEqualTo(effectivePrice, minPrice));
                }
                if (maxPrice != null) {
                    variantPreds.add(criteriaBuilder.lessThanOrEqualTo(effectivePrice, maxPrice));
                }

                variantSubquery.select(variantRoot.get("variantId"))
                        .where(variantPreds.toArray(new Predicate[0]));
                predicates.add(criteriaBuilder.exists(variantSubquery));
            }

            // Always exclude soft-deleted products from normal listings
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
