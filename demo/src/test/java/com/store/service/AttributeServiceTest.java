package com.store.service;

import com.store.dto.request.attribute.AttributeRequest;
import com.store.dto.response.attribute.AttributeResponse;
import com.store.entity.category.Category;
import com.store.entity.product.Attribute;
import com.store.entity.product.AttributeDataType;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.AttributeRepository;
import com.store.repository.CategoryRepository;
import com.store.service.impl.AttributeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttributeServiceTest {

    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AttributeServiceImpl attributeService;

    private Category category;
    private Attribute attribute;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(10)
                .name("CPU - Bộ vi xử lý")
                .slug("cpu-bo-vi-xu-ly")
                .build();

        attribute = Attribute.builder()
                .attributeId(1)
                .category(category)
                .name("Socket")
                .dataType(AttributeDataType.TEXT)
                .unit(null)
                .sortOrder(1)
                .build();
    }

    @Nested
    @DisplayName("Read Tests")
    class ReadTests {
        @Test
        void getAttributesByCategory_success() {
            when(attributeRepository.findByCategoryCategoryIdOrderBySortOrderAscAttributeIdAsc(10))
                    .thenReturn(List.of(attribute));

            List<AttributeResponse> result = attributeService.getAttributesByCategory(10);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Socket");
        }

        @Test
        void getAttributeById_found() {
            when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute));

            AttributeResponse result = attributeService.getAttributeById(1);
            assertThat(result.getAttributeId()).isEqualTo(1);
            assertThat(result.getName()).isEqualTo("Socket");
        }

        @Test
        void getAttributeById_notFound() {
            when(attributeRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attributeService.getAttributeById(999))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Create & Update Tests")
    class CreateUpdateTests {
        @Test
        void createAttribute_success() {
            AttributeRequest request = AttributeRequest.builder()
                    .categoryId(10)
                    .name("Số nhân")
                    .dataType(AttributeDataType.NUMBER)
                    .unit("Nhân")
                    .sortOrder(2)
                    .build();

            when(categoryRepository.findById(10)).thenReturn(Optional.of(category));
            when(attributeRepository.existsByCategoryCategoryIdAndNameIgnoreCase(10, "Số nhân")).thenReturn(false);
            when(attributeRepository.save(any(Attribute.class))).thenAnswer(inv -> {
                Attribute a = inv.getArgument(0);
                a.setAttributeId(2);
                return a;
            });

            AttributeResponse result = attributeService.createAttribute(request);
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Số nhân");
            assertThat(result.getDataType()).isEqualTo(AttributeDataType.NUMBER);
        }

        @Test
        void createAttribute_duplicateName_throws() {
            AttributeRequest request = AttributeRequest.builder()
                    .categoryId(10)
                    .name("Socket")
                    .dataType(AttributeDataType.TEXT)
                    .build();

            when(categoryRepository.findById(10)).thenReturn(Optional.of(category));
            when(attributeRepository.existsByCategoryCategoryIdAndNameIgnoreCase(10, "Socket")).thenReturn(true);

            assertThatThrownBy(() -> attributeService.createAttribute(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("đã tồn tại");
        }

        @Test
        void deleteAttribute_success() {
            when(attributeRepository.findById(1)).thenReturn(Optional.of(attribute));
            doNothing().when(attributeRepository).delete(attribute);

            attributeService.deleteAttribute(1);
            verify(attributeRepository, times(1)).delete(attribute);
        }
    }
}
