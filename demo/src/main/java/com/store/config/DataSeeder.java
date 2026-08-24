package com.store.config;

import com.store.entity.category.Category;
import com.store.entity.product.Attribute;
import com.store.entity.product.AttributeDataType;
import com.store.entity.product.Product;
import com.store.entity.product.ProductAttributeValue;
import com.store.entity.user.Permission;
import com.store.entity.user.Role;
import com.store.entity.user.User;
import com.store.entity.user.UserStatus;
import com.store.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final ProductRepository productRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    @Transactional
    public void run(String... args) {
        cleanupTestJunkCategories();
        seedPermissionsAndRoles();
        seedDefaultUsers();
        seedAttributesAndSpecifications();
    }

    private void cleanupTestJunkCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        List<Category> junk = allCategories.stream()
                .filter(c -> {
                    String name = c.getName() != null ? c.getName().trim() : "";
                    String slug = c.getSlug() != null ? c.getSlug().trim() : "";
                    return name.startsWith("Cat A") ||
                           name.startsWith("Cat B") ||
                           name.startsWith("Cat C") ||
                           name.startsWith("Cat Parent") ||
                           name.startsWith("Cat Child") ||
                           slug.startsWith("cat-a-") ||
                           slug.startsWith("cat-b-") ||
                           slug.startsWith("cat-c-") ||
                           slug.startsWith("cat-parent-") ||
                           slug.startsWith("cat-child-");
                })
                .toList();

        if (!junk.isEmpty()) {
            log.info("Cleaning up {} leftover test categories (Cat A, Cat B, etc.)...", junk.size());
            List<Category> sortedJunk = new ArrayList<>(junk);
            sortedJunk.sort((c1, c2) -> {
                int level1 = c1.getParent() == null ? 0 : (c1.getParent().getParent() == null ? 1 : 2);
                int level2 = c2.getParent() == null ? 0 : (c2.getParent().getParent() == null ? 1 : 2);
                return Integer.compare(level2, level1);
            });

            for (Category c : sortedJunk) {
                categoryRepository.delete(c);
            }
            log.info("Leftover test categories cleaned up successfully.");
        }
    }

    private void seedPermissionsAndRoles() {
        List<String> permissionCodes = List.of(
                "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE", "PRODUCT_VIEW",
                "CATEGORY_MANAGE", "BRAND_MANAGE",
                "ORDER_VIEW", "ORDER_MANAGE",
                "USER_VIEW", "USER_MANAGE"
        );

        Set<Permission> allPermissions = new HashSet<>();
        for (String code : permissionCodes) {
            Permission perm = permissionRepository.findByPermissionCode(code)
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .permissionCode(code)
                                    .description("Permission for " + code)
                                    .build()
                    ));
            allPermissions.add(perm);
        }

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_ADMIN")
                                .description("Full administrative access")
                                .permissions(allPermissions)
                                .build()
                ));
        if (adminRole.getPermissions().isEmpty()) {
            adminRole.setPermissions(allPermissions);
            roleRepository.save(adminRole);
        }

        roleRepository.findByRoleName("ROLE_STAFF")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_STAFF")
                                .description("Staff member access")
                                .permissions(new HashSet<>(allPermissions))
                                .build()
                ));

        roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName("ROLE_CUSTOMER")
                                .description("Standard shopping customer")
                                .build()
                ));
    }

    private void seedDefaultUsers() {
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElse(null);
        Role staffRole = roleRepository.findByRoleName("ROLE_STAFF").orElse(null);
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER").orElse(null);

        if (!userRepository.existsByEmail("admin@store.com")) {
            log.info("Seeding default Administrator account: admin@store.com");
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@store.com")
                    .phone("0988888888")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(adminRole != null ? Set.of(adminRole) : Set.of())
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("staff@store.com")) {
            log.info("Seeding default Staff account: staff@store.com");
            User staff = User.builder()
                    .fullName("Store Staff Member")
                    .email("staff@store.com")
                    .phone("0977777777")
                    .passwordHash(passwordEncoder.encode("Staff@123456"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(staffRole != null ? Set.of(staffRole) : Set.of())
                    .build();
            userRepository.save(staff);
        }

        if (!userRepository.existsByEmail("customer@store.com")) {
            log.info("Seeding default Customer account: customer@store.com");
            User customer = User.builder()
                    .fullName("Nguyễn Văn Khách")
                    .email("customer@store.com")
                    .phone("0966666666")
                    .passwordHash(passwordEncoder.encode("Customer@123456"))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(customerRole != null ? Set.of(customerRole) : Set.of())
                    .build();
            userRepository.save(customer);
        }
    }

    private void seedAttributesAndSpecifications() {
        // Check if domain-specific attributes (like "Socket" for CPU) are already in DB
        boolean hasCpuSocket = categoryRepository.findBySlug("cpu-bo-vi-xu-ly")
                .map(cat -> attributeRepository.existsByCategoryCategoryIdAndNameIgnoreCase(cat.getCategoryId(), "Socket"))
                .orElse(false);

        if (hasCpuSocket && productAttributeValueRepository.count() > 0) {
            log.info("Domain-specific technical attributes already seeded. Skipping re-seed.");
            return;
        }

        log.info("Resetting generic attributes and seeding specialized hardware technical attributes (EAV)...");
        productAttributeValueRepository.deleteAllInBatch();
        attributeRepository.deleteAllInBatch();

        Map<String, List<AttrDef>> categoryAttrDefs = new LinkedHashMap<>();

        // 1. CPU - Bộ vi xử lý
        categoryAttrDefs.put("cpu-bo-vi-xu-ly", List.of(
                new AttrDef("Socket", AttributeDataType.TEXT, null, 1),
                new AttrDef("Số nhân", AttributeDataType.NUMBER, "Nhân", 2),
                new AttrDef("Số luồng", AttributeDataType.NUMBER, "Luồng", 3),
                new AttrDef("Xung nhịp Turbo tối đa", AttributeDataType.TEXT, "GHz", 4),
                new AttrDef("Bộ nhớ đệm Cache", AttributeDataType.TEXT, "MB", 5),
                new AttrDef("TDP", AttributeDataType.NUMBER, "W", 6)
        ));

        // 2. VGA - Card màn hình
        categoryAttrDefs.put("vga-card-man-hinh", List.of(
                new AttrDef("Dung lượng VRAM", AttributeDataType.TEXT, "GB", 1),
                new AttrDef("Chuẩn bộ nhớ", AttributeDataType.TEXT, null, 2),
                new AttrDef("Công suất nguồn đề xuất", AttributeDataType.NUMBER, "W", 3),
                new AttrDef("Cổng xuất hình", AttributeDataType.TEXT, null, 4)
        ));

        // 3. RAM - Bộ nhớ trong
        categoryAttrDefs.put("ram-bo-nho-trong", List.of(
                new AttrDef("Chuẩn RAM", AttributeDataType.TEXT, null, 1),
                new AttrDef("Dung lượng", AttributeDataType.TEXT, "GB", 2),
                new AttrDef("Bus RAM", AttributeDataType.TEXT, "MHz", 3),
                new AttrDef("Độ trễ (CAS Latency)", AttributeDataType.TEXT, null, 4)
        ));

        // 4. Ổ cứng SSD & HDD
        categoryAttrDefs.put("o-cung-ssd-hdd", List.of(
                new AttrDef("Chuẩn kết nối", AttributeDataType.TEXT, null, 1),
                new AttrDef("Dung lượng", AttributeDataType.TEXT, null, 2),
                new AttrDef("Tốc độ đọc tối đa", AttributeDataType.TEXT, "MB/s", 3),
                new AttrDef("Tốc độ ghi tối đa", AttributeDataType.TEXT, "MB/s", 4)
        ));

        // 5. Mainboard - Bo mạch chủ
        categoryAttrDefs.put("mainboard-bo-mach-chu", List.of(
                new AttrDef("Socket", AttributeDataType.TEXT, null, 1),
                new AttrDef("Chipset", AttributeDataType.TEXT, null, 2),
                new AttrDef("Chuẩn RAM", AttributeDataType.TEXT, null, 3),
                new AttrDef("Kích thước (Form Factor)", AttributeDataType.TEXT, null, 4)
        ));

        // 6. Nguồn PSU & Tản nhiệt
        categoryAttrDefs.put("nguon-psu-tan-nhiet", List.of(
                new AttrDef("Công suất", AttributeDataType.NUMBER, "W", 1),
                new AttrDef("Chuẩn hiệu suất 80 Plus", AttributeDataType.TEXT, null, 2),
                new AttrDef("Kiểu cáp nguồn", AttributeDataType.TEXT, null, 3),
                new AttrDef("Loại tản nhiệt", AttributeDataType.TEXT, null, 4)
        ));

        // 7. Laptop & MacBooks
        List<AttrDef> laptopAttrs = List.of(
                new AttrDef("Kích thước màn hình", AttributeDataType.TEXT, "inch", 1),
                new AttrDef("Độ phân giải & Tần số quét", AttributeDataType.TEXT, null, 2),
                new AttrDef("Bộ vi xử lý (CPU)", AttributeDataType.TEXT, null, 3),
                new AttrDef("Card đồ họa (GPU)", AttributeDataType.TEXT, null, 4),
                new AttrDef("Dung lượng RAM", AttributeDataType.TEXT, "GB", 5),
                new AttrDef("Ổ cứng lưu trữ", AttributeDataType.TEXT, null, 6),
                new AttrDef("Trọng lượng", AttributeDataType.TEXT, "kg", 7)
        );
        categoryAttrDefs.put("laptop", laptopAttrs);
        categoryAttrDefs.put("macbook-apple", laptopAttrs);
        categoryAttrDefs.put("laptop-gaming", laptopAttrs);
        categoryAttrDefs.put("laptop-van-phong-sinh-vien", laptopAttrs);
        categoryAttrDefs.put("laptop-do-hoa-ky-thuat", laptopAttrs);
        categoryAttrDefs.put("laptop-mong-nhe-cao-cap", laptopAttrs);
        categoryAttrDefs.put("laptop-cam-ung-2-trong-1", laptopAttrs);

        // 8. Máy tính để bàn PC
        List<AttrDef> pcAttrs = List.of(
                new AttrDef("Bộ vi xử lý (CPU)", AttributeDataType.TEXT, null, 1),
                new AttrDef("Dung lượng RAM", AttributeDataType.TEXT, "GB", 2),
                new AttrDef("Ổ cứng lưu trữ", AttributeDataType.TEXT, null, 3),
                new AttrDef("Card đồ họa (VGA)", AttributeDataType.TEXT, null, 4),
                new AttrDef("Nguồn PSU", AttributeDataType.NUMBER, "W", 5),
                new AttrDef("Hệ điều hành", AttributeDataType.TEXT, null, 6)
        );
        categoryAttrDefs.put("may-tinh-de-ban-pc", pcAttrs);
        categoryAttrDefs.put("pc-gaming-streamer", pcAttrs);
        categoryAttrDefs.put("pc-do-hoa-workstation", pcAttrs);
        categoryAttrDefs.put("pc-van-phong-doanh-nghiep", pcAttrs);
        categoryAttrDefs.put("may-tinh-all-in-one", pcAttrs);
        categoryAttrDefs.put("mac-mini-mac-studio", pcAttrs);

        // 9. Màn hình
        List<AttrDef> monitorAttrs = List.of(
                new AttrDef("Kích thước", AttributeDataType.TEXT, "inch", 1),
                new AttrDef("Độ phân giải", AttributeDataType.TEXT, null, 2),
                new AttrDef("Tấm nền", AttributeDataType.TEXT, null, 3),
                new AttrDef("Tần số quét", AttributeDataType.TEXT, "Hz", 4),
                new AttrDef("Thời gian phản hồi", AttributeDataType.TEXT, "ms", 5),
                new AttrDef("Chuẩn màu", AttributeDataType.TEXT, null, 6)
        );
        categoryAttrDefs.put("man-hinh-may-tinh", monitorAttrs);
        categoryAttrDefs.put("man-hinh-gaming", monitorAttrs);
        categoryAttrDefs.put("man-hinh-do-hoa", monitorAttrs);
        categoryAttrDefs.put("man-hinh-van-phong", monitorAttrs);
        categoryAttrDefs.put("man-hinh-cong", monitorAttrs);

        // 10. Bàn phím cơ & Bàn phím văn phòng
        categoryAttrDefs.put("ban-phim-co-van-phong", List.of(
                new AttrDef("Loại kết nối", AttributeDataType.TEXT, null, 1),
                new AttrDef("Loại Switch", AttributeDataType.TEXT, null, 2),
                new AttrDef("Layout", AttributeDataType.TEXT, null, 3),
                new AttrDef("LED RGB", AttributeDataType.TEXT, null, 4)
        ));

        // 11. Chuột Gaming & Chuột không dây
        categoryAttrDefs.put("chuot-gaming-khong-day", List.of(
                new AttrDef("Cảm biến (Sensor)", AttributeDataType.TEXT, null, 1),
                new AttrDef("DPI tối đa", AttributeDataType.NUMBER, "DPI", 2),
                new AttrDef("Trọng lượng", AttributeDataType.TEXT, "gram", 3),
                new AttrDef("Kết nối", AttributeDataType.TEXT, null, 4)
        ));

        // 12. Tai nghe & Loa máy tính
        categoryAttrDefs.put("tai-nghe-loa-may-tinh", List.of(
                new AttrDef("Kiểu kết nối", AttributeDataType.TEXT, null, 1),
                new AttrDef("Thời lượng pin", AttributeDataType.TEXT, "giờ", 2),
                new AttrDef("Công nghệ âm thanh", AttributeDataType.TEXT, null, 3)
        ));

        // 13. Cáp sạc & Hub chuyển đổi
        categoryAttrDefs.put("cap-sac-hub-chuyen-doi", List.of(
                new AttrDef("Cổng đầu vào (Input)", AttributeDataType.TEXT, null, 1),
                new AttrDef("Cổng đầu ra (Output)", AttributeDataType.TEXT, null, 2),
                new AttrDef("Công suất sạc tối đa", AttributeDataType.NUMBER, "W", 3)
        ));

        Map<String, Map<String, Attribute>> categoryAttributeMap = new HashMap<>();

        for (Map.Entry<String, List<AttrDef>> entry : categoryAttrDefs.entrySet()) {
            String catSlug = entry.getKey();
            categoryRepository.findBySlug(catSlug).ifPresent(cat -> {
                Map<String, Attribute> attrMap = new HashMap<>();
                for (AttrDef def : entry.getValue()) {
                    Attribute attr = attributeRepository.save(
                            Attribute.builder()
                                    .category(cat)
                                    .name(def.name())
                                    .dataType(def.dataType())
                                    .unit(def.unit())
                                    .sortOrder(def.sortOrder())
                                    .build()
                    );
                    attrMap.put(def.name(), attr);
                }
                categoryAttributeMap.put(catSlug, attrMap);
            });
        }

        // Now map specs to existing products
        List<Product> products = productRepository.findAll();
        List<ProductAttributeValue> pavList = new ArrayList<>();

        for (Product p : products) {
            if (p.getCategory() == null) continue;
            String catSlug = p.getCategory().getSlug();
            Map<String, Attribute> attrs = categoryAttributeMap.get(catSlug);
            if (attrs == null && p.getCategory().getParent() != null) {
                attrs = categoryAttributeMap.get(p.getCategory().getParent().getSlug());
            }
            if (attrs == null) continue;

            String nameLower = p.getName().toLowerCase();

            // CPU
            if (catSlug.contains("cpu")) {
                if (attrs.containsKey("Socket")) {
                    String socket = nameLower.contains("intel") || nameLower.contains("i9") || nameLower.contains("i7") || nameLower.contains("i5") ? "LGA1700" : "AM5";
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Socket")).value(socket).build());
                }
                if (attrs.containsKey("Số nhân")) {
                    String cores = nameLower.contains("i9") ? "24" : (nameLower.contains("i7") ? "20" : (nameLower.contains("i5") ? "14" : (nameLower.contains("7950x") ? "16" : "8")));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Số nhân")).value(cores).build());
                }
                if (attrs.containsKey("Số luồng")) {
                    String threads = nameLower.contains("i9") ? "32" : (nameLower.contains("i7") ? "28" : (nameLower.contains("i5") ? "20" : "16"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Số luồng")).value(threads).build());
                }
                if (attrs.containsKey("Xung nhịp Turbo tối đa")) {
                    String boost = nameLower.contains("14900k") ? "6.0 GHz" : (nameLower.contains("14700k") ? "5.6 GHz" : (nameLower.contains("7950x") ? "5.7 GHz" : "5.4 GHz"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Xung nhịp Turbo tối đa")).value(boost).build());
                }
                if (attrs.containsKey("TDP")) {
                    String tdp = nameLower.contains("k") || nameLower.contains("x") ? "125" : "65";
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("TDP")).value(tdp).build());
                }
            }
            // VGA
            else if (catSlug.contains("vga")) {
                if (attrs.containsKey("Dung lượng VRAM")) {
                    String vram = nameLower.contains("4090") ? "24GB" : (nameLower.contains("4080") ? "16GB" : (nameLower.contains("4070") ? "12GB" : "8GB"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Dung lượng VRAM")).value(vram).build());
                }
                if (attrs.containsKey("Chuẩn bộ nhớ")) {
                    String mem = nameLower.contains("40") ? "GDDR6X" : "GDDR6";
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Chuẩn bộ nhớ")).value(mem).build());
                }
                if (attrs.containsKey("Công suất nguồn đề xuất")) {
                    String psu = nameLower.contains("4090") ? "1000" : (nameLower.contains("4080") ? "850" : (nameLower.contains("4070") ? "750" : "650"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Công suất nguồn đề xuất")).value(psu).build());
                }
                if (attrs.containsKey("Cổng xuất hình")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Cổng xuất hình")).value("3x DisplayPort 1.4a, 1x HDMI 2.1a").build());
                }
            }
            // RAM
            else if (catSlug.contains("ram")) {
                if (attrs.containsKey("Chuẩn RAM")) {
                    String type = nameLower.contains("ddr4") ? "DDR4" : "DDR5";
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Chuẩn RAM")).value(type).build());
                }
                if (attrs.containsKey("Dung lượng")) {
                    String cap = nameLower.contains("64gb") ? "64GB (2x32GB)" : (nameLower.contains("32gb") ? "32GB (2x16GB)" : "16GB (2x8GB)");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Dung lượng")).value(cap).build());
                }
                if (attrs.containsKey("Bus RAM")) {
                    String bus = nameLower.contains("6000") ? "6000MHz" : (nameLower.contains("5600") ? "5600MHz" : "3200MHz");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Bus RAM")).value(bus).build());
                }
            }
            // Laptop & MacBook
            else if (catSlug.contains("laptop") || catSlug.contains("macbook")) {
                if (attrs.containsKey("Kích thước màn hình")) {
                    String size = nameLower.contains("16") || nameLower.contains("g16") ? "16 inch" : (nameLower.contains("14") ? "14 inch" : (nameLower.contains("13") ? "13.6 inch" : "15.6 inch"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Kích thước màn hình")).value(size).build());
                }
                if (attrs.containsKey("Độ phân giải & Tần số quét")) {
                    String res = nameLower.contains("rog") || nameLower.contains("legion") ? "QHD+ 240Hz 100% DCI-P3" : (nameLower.contains("macbook") ? "Liquid Retina XDR ProMotion 120Hz" : "FHD 144Hz IPS");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Độ phân giải & Tần số quét")).value(res).build());
                }
                if (attrs.containsKey("Bộ vi xử lý (CPU)")) {
                    String cpu = nameLower.contains("macbook") ? "Apple M3 Pro (12-Core CPU)" : (nameLower.contains("i9") ? "Intel Core i9-14900HX" : "Intel Core i7-14700HX");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Bộ vi xử lý (CPU)")).value(cpu).build());
                }
                if (attrs.containsKey("Card đồ họa (GPU)")) {
                    String gpu = nameLower.contains("macbook") ? "Apple M3 Pro 18-Core GPU" : (nameLower.contains("4080") ? "NVIDIA GeForce RTX 4080 12GB" : (nameLower.contains("4070") ? "NVIDIA GeForce RTX 4070 8GB" : "NVIDIA GeForce RTX 4060 8GB"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Card đồ họa (GPU)")).value(gpu).build());
                }
                if (attrs.containsKey("Trọng lượng")) {
                    String weight = nameLower.contains("macbook") ? "1.24 kg" : (nameLower.contains("rog") || nameLower.contains("legion") ? "2.4 kg" : "1.8 kg");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Trọng lượng")).value(weight).build());
                }
            }
            // Monitor
            else if (catSlug.contains("man-hinh")) {
                if (attrs.containsKey("Kích thước")) {
                    String size = nameLower.contains("32") ? "32 inch" : (nameLower.contains("24") ? "24 inch" : "27 inch");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Kích thước")).value(size).build());
                }
                if (attrs.containsKey("Độ phân giải")) {
                    String res = nameLower.contains("4k") ? "4K UHD (3840x2160)" : (nameLower.contains("2k") || nameLower.contains("qhd") ? "2K QHD (2560x1440)" : "FHD (1920x1080)");
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Độ phân giải")).value(res).build());
                }
                if (attrs.containsKey("Tấm nền")) {
                    String panel = nameLower.contains("oled") ? "QD-OLED" : "Fast IPS";
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Tấm nền")).value(panel).build());
                }
                if (attrs.containsKey("Tần số quét")) {
                    String hz = nameLower.contains("240") ? "240Hz" : (nameLower.contains("180") ? "180Hz" : (nameLower.contains("165") ? "165Hz" : "144Hz"));
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Tần số quét")).value(hz).build());
                }
                if (attrs.containsKey("Chuẩn màu")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Chuẩn màu")).value("99% DCI-P3, Delta E < 2").build());
                }
            }
            // SSD
            else if (catSlug.contains("ssd")) {
                if (attrs.containsKey("Chuẩn kết nối")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Chuẩn kết nối")).value("M.2 NVMe PCIe 4.0 x4").build());
                }
                if (attrs.containsKey("Tốc độ đọc tối đa")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Tốc độ đọc tối đa")).value("7450 MB/s").build());
                }
                if (attrs.containsKey("Tốc độ ghi tối đa")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Tốc độ ghi tối đa")).value("6900 MB/s").build());
                }
            }
            // Bàn phím
            else if (catSlug.contains("ban-phim")) {
                if (attrs.containsKey("Loại kết nối")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Loại kết nối")).value("Wireless 2.4GHz / Bluetooth 5.1 / Type-C").build());
                }
                if (attrs.containsKey("Loại Switch")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Loại Switch")).value("Linear Hot-swappable Pre-lubed").build());
                }
                if (attrs.containsKey("Layout")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Layout")).value("75% (81 Phím)").build());
                }
                if (attrs.containsKey("LED RGB")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("LED RGB")).value("RGB 16.8 triệu màu Per-Key").build());
                }
            }
            // Chuột
            else if (catSlug.contains("chuot")) {
                if (attrs.containsKey("Cảm biến (Sensor)")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Cảm biến (Sensor)")).value("Optical Gaming Sensor 30K").build());
                }
                if (attrs.containsKey("DPI tối đa")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("DPI tối đa")).value("30000").build());
                }
                if (attrs.containsKey("Trọng lượng")) {
                    pavList.add(ProductAttributeValue.builder().product(p).attribute(attrs.get("Trọng lượng")).value("58g siêu nhẹ").build());
                }
            }
        }

        if (!pavList.isEmpty()) {
            productAttributeValueRepository.saveAll(pavList);
            log.info("Successfully seeded {} authentic product specifications values!", pavList.size());
        }
    }

    private record AttrDef(String name, AttributeDataType dataType, String unit, int sortOrder) {}
}
