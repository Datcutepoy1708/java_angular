package com.store.util;

import com.store.entity.brand.Brand;
import com.store.entity.category.Category;
import com.store.entity.category.CategoryStatus;
import com.store.entity.inventory.Warehouse;
import com.store.entity.product.Product;
import com.store.entity.product.ProductStatus;
import com.store.entity.product.ProductVariant;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Self-contained test fixture helper ensuring minimal test domain entities
 * (Warehouses, Category, Brand, Product, Variant, Customer User) exist
 * when running tests against an empty database (e.g. fresh CI container).
 */
@Component
@RequiredArgsConstructor
public class TestFixtureHelper {

    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public void ensureBasicFixtures() {
        ensureWarehouses();
        ensureProductAndVariant();
        ensureCustomerUser();
    }

    private void ensureWarehouses() {
        if (warehouseRepository.count() < 2) {
            warehouseRepository.save(Warehouse.builder()
                    .name("Kho Tổng Miền Bắc (Hà Nội)")
                    .address("Số 120 Thái Hà, Đống Đa, Hà Nội")
                    .phone("02438570512")
                    .build());
            warehouseRepository.save(Warehouse.builder()
                    .name("Kho Tổng Miền Nam (TP.HCM)")
                    .address("Số 215 Trần Quang Khải, Quận 1, TP.HCM")
                    .phone("02838206888")
                    .build());
        }
    }

    private void ensureProductAndVariant() {
        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(Category.builder()
                        .name("Linh kiện máy tính")
                        .slug("linh-kien-may-tinh-" + System.nanoTime())
                        .status(CategoryStatus.ACTIVE)
                        .build())
        );

        Brand brand = brandRepository.findAll().stream().findFirst().orElseGet(() ->
                brandRepository.save(Brand.builder()
                        .name("ASUS")
                        .slug("asus-" + System.nanoTime())
                        .build())
        );

        Product product = productRepository.findAll().stream().findFirst().orElseGet(() ->
                productRepository.save(Product.builder()
                        .category(category)
                        .brand(brand)
                        .name("VGA ASUS ROG Strix GeForce RTX 4090")
                        .slug("vga-asus-rog-strix-geforce-rtx-4090-" + System.nanoTime())
                        .sku("VGA-RTX4090-ROG-" + (System.nanoTime() % 100000))
                        .status(ProductStatus.ACTIVE)
                        .warrantyMonths(36)
                        .build())
        );

        while (productVariantRepository.count() < 3) {
            long idx = productVariantRepository.count() + 1;
            productVariantRepository.save(ProductVariant.builder()
                    .product(product)
                    .variantName("ASUS ROG Strix RTX 4090 Variant " + idx)
                    .skuVariant("ROG-STRIX-RTX4090-V" + idx + "-" + (System.nanoTime() % 100000))
                    .price(new BigDecimal("49990000.00"))
                    .salePrice(new BigDecimal("47990000.00"))
                    .costPrice(new BigDecimal("42000000.00"))
                    .status(com.store.entity.product.ProductVariantStatus.ACTIVE)
                    .build());
        }
    }

    private void ensureCustomerUser() {
        if (userRepository.findAll().isEmpty()) {
            Role roleCustomer = roleRepository.findByRoleName("ROLE_CUSTOMER").orElse(null);
            Set<Role> roles = new HashSet<>();
            if (roleCustomer != null) {
                roles.add(roleCustomer);
            }

            userRepository.save(User.builder()
                    .fullName("Customer Test")
                    .email("customer.test@example.com")
                    .phone("0987654321")
                    .passwordHash("$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW")
                    .status(com.store.entity.user.UserStatus.ACTIVE)
                    .roles(roles)
                    .build());
        }
    }
}
