package com.store.service;

import com.store.dto.response.PageResponse;
import com.store.dto.supplier.SupplierRequest;
import com.store.dto.supplier.SupplierResponse;
import com.store.entity.supplier.Supplier;
import com.store.entity.supplier.SupplierStatus;
import com.store.exception.BadRequestException;
import com.store.exception.DuplicateResourceException;
import com.store.exception.ResourceNotFoundException;
import com.store.repository.ProductRepository;
import com.store.repository.SupplierRepository;
import com.store.service.impl.SupplierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .supplierId(1)
                .name("Công ty TNHH ASUS Việt Nam")
                .contactName("Nguyễn Văn A")
                .phone("0901234567")
                .email("contact@asus.vn")
                .address("Tầng 5, Tòa nhà Viettel, Hà Nội")
                .status(SupplierStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Read Operations")
    class ReadTests {

        @Test
        @DisplayName("getAllActiveSuppliers should return active suppliers with product counts")
        void getAllActiveSuppliers_Success() {
            when(supplierRepository.findAllActive()).thenReturn(List.of(testSupplier));
            when(productRepository.countBySupplier_SupplierId(1)).thenReturn(5L);

            List<SupplierResponse> result = supplierService.getAllActiveSuppliers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Công ty TNHH ASUS Việt Nam");
            assertThat(result.get(0).getProductCount()).isEqualTo(5L);
            verify(supplierRepository).findAllActive();
        }

        @Test
        @DisplayName("getSuppliersPaginated should return page response")
        void getSuppliersPaginated_Success() {
            Page<Supplier> page = new PageImpl<>(List.of(testSupplier));
            when(supplierRepository.findAllFiltered(eq("asus"), eq(SupplierStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(page);
            when(productRepository.countBySupplier_SupplierId(1)).thenReturn(3L);

            PageResponse<SupplierResponse> response = supplierService.getSuppliersPaginated(
                    0, 10, "asus", "active", "createdAt", "desc"
            );

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getName()).isEqualTo("Công ty TNHH ASUS Việt Nam");
            assertThat(response.getTotalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getSupplierById should return supplier when found")
        void getSupplierById_Success() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productRepository.countBySupplier_SupplierId(1)).thenReturn(2L);

            SupplierResponse response = supplierService.getSupplierById(1);

            assertThat(response).isNotNull();
            assertThat(response.getSupplierId()).isEqualTo(1);
            assertThat(response.getProductCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("getSupplierById should throw ResourceNotFoundException when not found")
        void getSupplierById_NotFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.getSupplierById(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("Create & Update Operations")
    class CreateUpdateTests {

        @Test
        @DisplayName("createSupplier should succeed when name is unique")
        void createSupplier_Success() {
            SupplierRequest request = SupplierRequest.builder()
                    .name("Công ty FPT Synnex")
                    .contactName("Trần Văn B")
                    .phone("0987654321")
                    .email("fpt@synnex.vn")
                    .address("Hà Nội")
                    .status("active")
                    .build();

            when(supplierRepository.existsByName("Công ty FPT Synnex")).thenReturn(false);
            when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
                Supplier s = invocation.getArgument(0);
                s.setSupplierId(2);
                return s;
            });

            SupplierResponse response = supplierService.createSupplier(request);

            assertThat(response).isNotNull();
            assertThat(response.getSupplierId()).isEqualTo(2);
            assertThat(response.getName()).isEqualTo("Công ty FPT Synnex");
            verify(supplierRepository).save(any(Supplier.class));
        }

        @Test
        @DisplayName("createSupplier should throw DuplicateResourceException when name already exists")
        void createSupplier_DuplicateName() {
            SupplierRequest request = SupplierRequest.builder()
                    .name("Công ty TNHH ASUS Việt Nam")
                    .build();

            when(supplierRepository.existsByName("Công ty TNHH ASUS Việt Nam")).thenReturn(true);

            assertThatThrownBy(() -> supplierService.createSupplier(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("đã tồn tại");

            verify(supplierRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateSupplier should succeed when data is valid")
        void updateSupplier_Success() {
            SupplierRequest request = SupplierRequest.builder()
                    .name("ASUS Vietnam Co., Ltd")
                    .contactName("Lê Văn C")
                    .phone("0911223344")
                    .email("updated@asus.vn")
                    .address("TP Hồ Chí Minh")
                    .status("inactive")
                    .build();

            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(supplierRepository.existsByNameAndSupplierIdNot("ASUS Vietnam Co., Ltd", 1)).thenReturn(false);
            when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> i.getArgument(0));
            when(productRepository.countBySupplier_SupplierId(1)).thenReturn(4L);

            SupplierResponse response = supplierService.updateSupplier(1, request);

            assertThat(response.getName()).isEqualTo("ASUS Vietnam Co., Ltd");
            assertThat(response.getStatus()).isEqualTo("inactive");
            assertThat(response.getProductCount()).isEqualTo(4L);
        }

        @Test
        @DisplayName("updateSupplier should throw DuplicateResourceException when new name conflicts")
        void updateSupplier_DuplicateName() {
            SupplierRequest request = SupplierRequest.builder()
                    .name("Existing Supplier")
                    .build();

            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(supplierRepository.existsByNameAndSupplierIdNot("Existing Supplier", 1)).thenReturn(true);

            assertThatThrownBy(() -> supplierService.updateSupplier(1, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("sử dụng");

            verify(supplierRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("deleteSupplier should succeed when no products are associated")
        void deleteSupplier_Success() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productRepository.countBySupplier_SupplierId(1)).thenReturn(0L);

            supplierService.deleteSupplier(1);

            verify(supplierRepository).delete(testSupplier);
        }

        @Test
        @DisplayName("deleteSupplier should throw BadRequestException when products are linked")
        void deleteSupplier_LinkedProducts() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productRepository.countBySupplier_SupplierId(1)).thenReturn(8L);

            assertThatThrownBy(() -> supplierService.deleteSupplier(1))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("8 sản phẩm liên kết");

            verify(supplierRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteSupplier should throw ResourceNotFoundException when not found")
        void deleteSupplier_NotFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supplierService.deleteSupplier(99))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(supplierRepository, never()).delete(any());
        }
    }
}
