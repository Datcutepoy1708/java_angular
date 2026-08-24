package com.store.service;

import com.store.dto.request.attribute.BatchSaveProductAttributesRequest;
import com.store.dto.request.attribute.ProductAttributeValueRequest;
import com.store.dto.response.attribute.ProductAttributeValueResponse;
import com.store.entity.category.Category;
import com.store.entity.product.Attribute;
import com.store.entity.product.AttributeDataType;
import com.store.entity.product.Product;
import com.store.entity.product.ProductAttributeValue;
import com.store.repository.AttributeRepository;
import com.store.repository.ProductAttributeValueRepository;
import com.store.repository.ProductRepository;
import com.store.service.impl.ProductAttributeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductAttributeServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ProductAttributeValueRepository productAttributeValueRepository;

    @InjectMocks
    private ProductAttributeServiceImpl productAttributeService;

    private Product product;
    private Attribute attribute;
    private ProductAttributeValue pav;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().categoryId(10).name("CPU").build();
        product = Product.builder().productId(100L).name("Intel Core i9-14900K").category(category).build();
        attribute = Attribute.builder().attributeId(1).name("Socket").dataType(AttributeDataType.TEXT).build();
        pav = ProductAttributeValue.builder().id(1L).product(product).attribute(attribute).value("LGA1700").build();
    }

    @Test
    @DisplayName("getProductAttributes returns mapped list")
    void getProductAttributes_success() {
        when(productRepository.existsById(100L)).thenReturn(true);
        when(productAttributeValueRepository.findByProductProductId(100L)).thenReturn(List.of(pav));

        List<ProductAttributeValueResponse> result = productAttributeService.getProductAttributes(100L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getValue()).isEqualTo("LGA1700");
        assertThat(result.get(0).getAttributeName()).isEqualTo("Socket");
    }

    @Test
    @DisplayName("saveProductAttributes deletes old and saves batch")
    void saveProductAttributes_success() {
        BatchSaveProductAttributesRequest request = BatchSaveProductAttributesRequest.builder()
                .attributes(List.of(
                        ProductAttributeValueRequest.builder().attributeId(1).value("LGA1700").build()
                ))
                .build();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute));
        when(productAttributeValueRepository.saveAll(anyList())).thenReturn(List.of(pav));

        List<ProductAttributeValueResponse> result = productAttributeService.saveProductAttributes(100L, request);
        assertThat(result).hasSize(1);
        verify(productAttributeValueRepository, times(1)).deleteByProductId(100L);
        verify(productAttributeValueRepository, times(1)).saveAll(anyList());
    }
}
