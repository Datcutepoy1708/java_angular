package com.store.service;

import com.store.dto.request.BrandRequest;
import com.store.dto.response.BrandResponse;
import com.store.entity.brand.Brand;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.BrandRepository;
import com.store.service.impl.BrandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand brand;
    private BrandRequest brandRequest;

    @BeforeEach
    void setUp() {
        brand = Brand.builder()
                .brandId(1)
                .name("ASUS")
                .slug("asus")
                .logoUrl("https://example.com/asus.png")
                .country("Taiwan")
                .description("In Search of Incredible")
                .build();

        brandRequest = BrandRequest.builder()
                .name("ASUS")
                .slug("asus")
                .logoUrl("https://example.com/asus.png")
                .country("Taiwan")
                .description("In Search of Incredible")
                .build();
    }

    @Test
    @DisplayName("Should return all brands successfully")
    void testGetAllBrands() {
        when(brandRepository.findAll(any(Sort.class))).thenReturn(List.of(brand));

        List<BrandResponse> result = brandService.getAllBrands();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("ASUS");
        verify(brandRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("Should return brand by ID when brand exists")
    void testGetBrandById_Success() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand));

        BrandResponse result = brandService.getBrandById(1);

        assertThat(result).isNotNull();
        assertThat(result.getBrandId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("ASUS");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when brand ID not found")
    void testGetBrandById_NotFound() {
        when(brandRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getBrandById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Brand not found with id: 999");
    }

    @Test
    @DisplayName("Should create brand successfully")
    void testCreateBrand_Success() {
        when(brandRepository.existsByName("ASUS")).thenReturn(false);
        when(brandRepository.existsBySlug("asus")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        BrandResponse result = brandService.createBrand(brandRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ASUS");
        verify(brandRepository, times(1)).save(any(Brand.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when brand name already exists")
    void testCreateBrand_DuplicateName() {
        when(brandRepository.existsByName("ASUS")).thenReturn(true);

        assertThatThrownBy(() -> brandService.createBrand(brandRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Brand already exists with name: ASUS");
    }

    @Test
    @DisplayName("Should update brand successfully")
    void testUpdateBrand_Success() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndBrandIdNot("ASUS", 1)).thenReturn(false);
        when(brandRepository.existsBySlugAndBrandIdNot("asus", 1)).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);

        BrandResponse result = brandService.updateBrand(1, brandRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ASUS");
    }

    @Test
    @DisplayName("Should delete brand successfully")
    void testDeleteBrand_Success() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand));
        doNothing().when(brandRepository).delete(brand);

        brandService.deleteBrand(1);

        verify(brandRepository, times(1)).delete(brand);
    }
}
