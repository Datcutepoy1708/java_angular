package com.store.config;

import com.store.entity.banner.Banner;
import com.store.entity.banner.BannerPosition;
import com.store.entity.banner.BannerStatus;
import com.store.entity.news.News;
import com.store.entity.news.NewsCategory;
import com.store.entity.news.NewsStatus;
import com.store.entity.user.User;
import com.store.repository.BannerRepository;
import com.store.repository.NewsCategoryRepository;
import com.store.repository.NewsRepository;
import com.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically seeds initial dynamic Banners and Tech News (crawled from top tech portals)
 * if the corresponding tables are empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentDataSeeder implements CommandLineRunner {

    private final BannerRepository bannerRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedBannersIfEmpty();
        seedNewsIfEmpty();
    }

    private void seedBannersIfEmpty() {
        if (bannerRepository.count() > 0) {
            return;
        }

        log.info("Auto-seeding dynamic banners for homepage slider & marketing...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusYears(3);

        List<Banner> initialBanners = List.of(
                Banner.builder()
                        .title("SIÊU HỘI LAPTOP GAMING & AI RTX 40-SERIES")
                        .imageUrl("https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=1600&auto=format&fit=crop")
                        .linkUrl("/products?category=laptop-gaming")
                        .position(BannerPosition.HOMEPAGE_SLIDER)
                        .sortOrder(1)
                        .startDate(now.minusDays(7))
                        .endDate(future)
                        .status(BannerStatus.ACTIVE)
                        .build(),
                Banner.builder()
                        .title("BUILD PC WORKSTATION & GAMING CAO CẤP - TẶNG TẢN NHIỆT AIO 360")
                        .imageUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?q=80&w=1600&auto=format&fit=crop")
                        .linkUrl("/products?category=pc-gaming-streamer")
                        .position(BannerPosition.HOMEPAGE_SLIDER)
                        .sortOrder(2)
                        .startDate(now.minusDays(7))
                        .endDate(future)
                        .status(BannerStatus.ACTIVE)
                        .build(),
                Banner.builder()
                        .title("APPLE MACBOOK M3 SERIES CHÍNH HÃNG - TRỢ GIÁ ĐỔI CŨ LẤY MỚI")
                        .imageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1600&auto=format&fit=crop")
                        .linkUrl("/products?category=macbook-apple")
                        .position(BannerPosition.HOMEPAGE_SLIDER)
                        .sortOrder(3)
                        .startDate(now.minusDays(7))
                        .endDate(future)
                        .status(BannerStatus.ACTIVE)
                        .build(),
                Banner.builder()
                        .title("TUẦN LỄ LINH KIỆN PC: VGA RTX 4070 SUPER, RAM DDR5, SSD GEN 4 GIẢM 35%")
                        .imageUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=1600&auto=format&fit=crop")
                        .linkUrl("/products?category=linh-kien-may-tinh")
                        .position(BannerPosition.HOMEPAGE_SLIDER)
                        .sortOrder(4)
                        .startDate(now.minusDays(7))
                        .endDate(future)
                        .status(BannerStatus.ACTIVE)
                        .build(),
                Banner.builder()
                        .title("MÀN HÌNH GAMING FAST IPS 2K 240HZ - TRẢI NGHIỆM ĐỒ HỌA ĐỈNH CAO")
                        .imageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?q=80&w=1000&auto=format&fit=crop")
                        .linkUrl("/products?category=man-hinh-may-tinh")
                        .position(BannerPosition.SIDEBAR)
                        .sortOrder(1)
                        .startDate(now.minusDays(7))
                        .endDate(future)
                        .status(BannerStatus.ACTIVE)
                        .build()
        );

        bannerRepository.saveAll(initialBanners);
        log.info("Successfully seeded {} dynamic banners.", initialBanners.size());
    }

    private void seedNewsIfEmpty() {
        if (newsRepository.count() > 0) {
            return;
        }

        log.info("Auto-seeding tech news categories and articles...");
        User adminUser = userRepository.findAll().stream().findFirst().orElse(null);

        // 1. Categories
        NewsCategory reviewCat = newsCategoryRepository.findBySlug("danh-gia-review")
                .orElseGet(() -> newsCategoryRepository.save(NewsCategory.builder()
                        .name("Đánh giá & Review")
                        .slug("danh-gia-review")
                        .description("Chuyên mục đánh giá chi tiết phần cứng, laptop và linh kiện máy tính")
                        .sortOrder(1)
                        .status("active")
                        .build()));

        NewsCategory guideCat = newsCategoryRepository.findBySlug("thu-thuat-huong-dan")
                .orElseGet(() -> newsCategoryRepository.save(NewsCategory.builder()
                        .name("Thủ thuật & Hướng dẫn")
                        .slug("thu-thuat-huong-dan")
                        .description("Hướng dẫn lắp ráp máy tính (Build PC), tối ưu phần cứng và Windows")
                        .sortOrder(2)
                        .status("active")
                        .build()));

        NewsCategory techNewsCat = newsCategoryRepository.findBySlug("tin-tuc-cong-nghe")
                .orElseGet(() -> newsCategoryRepository.save(NewsCategory.builder()
                        .name("Tin tức Công nghệ")
                        .slug("tin-tuc-cong-nghe")
                        .description("Cập nhật xu hướng vi xử lý, card đồ họa và tin tức công nghệ mới")
                        .sortOrder(3)
                        .status("active")
                        .build()));

        NewsCategory adviceCat = newsCategoryRepository.findBySlug("tu-van-cau-hinh")
                .orElseGet(() -> newsCategoryRepository.save(NewsCategory.builder()
                        .name("Tư vấn Cấu hình")
                        .slug("tu-van-cau-hinh")
                        .description("Tư vấn cấu hình máy tính gaming, làm đồ họa và workstation")
                        .sortOrder(4)
                        .status("active")
                        .build()));

        // 2. Articles
        List<News> initialArticles = List.of(
                News.builder()
                        .title("Đánh giá chi tiết NVIDIA GeForce RTX 4070 Super: \"Ông vua\" đồ họa 2K và Gaming đỉnh cao 2026")
                        .slug("danh-gia-nvidia-geforce-rtx-4070-super-ong-vua-do-hoa-2k")
                        .category(reviewCat)
                        .author(adminUser)
                        .thumbnailUrl("https://images.unsplash.com/photo-1587202372775-e229f172b9d7?q=80&w=1000&auto=format&fit=crop")
                        .summary("GeForce RTX 4070 Super mang đến bước nhảy vọt về hiệu năng nhờ kiến trúc Ada Lovelace, 12GB VRAM GDDR6X cùng DLSS 3.5 Frame Generation, cân mượt mọi tựa game AAA ở độ phân giải 2K 144Hz.")
                        .content("<h2>Tổng quan về NVIDIA GeForce RTX 4070 Super</h2>" +
                                "<p>Kể từ khi ra mắt, dòng sản phẩm RTX 40-Series Super của NVIDIA đã nhanh chóng định hình lại phân khúc card màn hình tầm trung - cận cao cấp. Trong đó, <strong>RTX 4070 Super</strong> là cái tên nổi bật nhất nhờ mức hiệu năng tiệm cận với RTX 4070 Ti nhưng sở hữu mức giá hấp dẫn hơn đáng kể.</p>" +
                                "<h3>1. Thông số kỹ thuật ấn tượng</h3>" +
                                "<ul>" +
                                "<li><strong>Số nhân CUDA:</strong> 7.168 nhân (tăng gần 22% so với 5.888 nhân trên bản 4070 tiêu chuẩn).</li>" +
                                "<li><strong>Xung nhịp Boost:</strong> Lên đến 2.475 MHz.</li>" +
                                "<li><strong>Bộ nhớ VRAM:</strong> 12GB GDDR6X tốc độ 21 Gbps, bus 192-bit.</li>" +
                                "<li><strong>Mức tiêu thụ điện năng (TDP):</strong> Chỉ 220W, cực kỳ tiết kiệm điện năng nhờ tiến trình 4N TSMC.</li>" +
                                "</ul>" +
                                "<h3>2. Hiệu năng Gaming thực tế ở độ phân giải 2K (1440p)</h3>" +
                                "<p>Thử nghiệm trên hệ thống CPU Core i7-14700K và 32GB RAM DDR5 6000MHz cho thấy kết quả ấn tượng:</p>" +
                                "<ul>" +
                                "<li><strong>Cyberpunk 2077 (Ray Tracing Overdrive, DLSS 3.5 Quality):</strong> 95 - 110 FPS vô cùng mượt mà.</li>" +
                                "<li><strong>Black Myth: Wukong (Cinematic Setting, Full Ray Tracing):</strong> Đạt trung bình 82 FPS ở độ phân giải 2K.</li>" +
                                "<li><strong>Counter-Strike 2 & Valorant:</strong> Luôn duy trì trên 400 FPS, khai thác tối đa màn hình tần số quét cao 240Hz/360Hz.</li>" +
                                "</ul>" +
                                "<h3>3. Khả năng làm việc đồ họa chuyên nghiệp</h3>" +
                                "<p>Với bộ nhớ 12GB VRAM chuẩn GDDR6X kết hợp cùng các nhân RT Core Gen 3 và Tensor Core Gen 4, RTX 4070 Super xử lý xuất sắc các tác vụ render 3D trên Blender, biên tập timeline 4K/6K trên Adobe Premiere Pro và DaVinci Resolve Studio với thời gian xuất file nhanh hơn 35% so với thế hệ RTX 3070 trước đây.</p>" +
                                "<h3>4. Kết luận</h3>" +
                                "<p>NVIDIA GeForce RTX 4070 Super xứng đáng là lựa chọn hàng đầu cho các bộ PC Gaming & Workstation tầm trung - cao cấp hiện nay. Sản phẩm hiện đang được phân phối chính hãng tại hệ thống Complexus với chế độ bảo hành 36 tháng 1 đổi 1.</p>")
                        .viewCount(1420)
                        .status(NewsStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(2))
                        .build(),

                News.builder()
                        .title("So sánh Intel Core Gen 14th vs AMD Ryzen 9000 Series: Đâu là CPU tối ưu nhất cho Gaming và Render?")
                        .slug("so-sanh-intel-gen-14-vs-amd-ryzen-9000-series")
                        .category(techNewsCat)
                        .author(adminUser)
                        .thumbnailUrl("https://images.unsplash.com/photo-1555680202-c86f0e12f086?q=80&w=1000&auto=format&fit=crop")
                        .summary("Cuộc chạm trán nảy lửa giữa kiến trúc Raptor Lake Refresh của Intel và Zen 5 của AMD. Phân tích chi tiết mức tiêu thụ điện năng, nhiệt độ và hiệu năng đơn nhân/đa nhân thực tế.")
                        .content("<h2>Cuộc đua vi xử lý năm 2026: Intel hay AMD?</h2>" +
                                "<p>Thị trường CPU máy tính để bàn chứng kiến sự cạnh tranh quyết liệt giữa dòng chip <strong>Intel Core Gen 14th (Raptor Lake Refresh)</strong> và đối thủ truyền kiếp <strong>AMD Ryzen 9000 Series (Kiến trúc Zen 5)</strong>. Cả hai đều mang đến những cải tiến vượt bậc về IPC và xung nhịp.</p>" +
                                "<h3>1. Kiến trúc và hiệu năng IPC</h3>" +
                                "<p>Trong khi Intel tiếp tục hoàn thiện kiến trúc lai kết hợp giữa Performance Cores (P-Cores) và Efficient Cores (E-Cores) mang lại khả năng đa nhiệm tuyệt vời cho các phần mềm dựng hình đa luồng, thì AMD với tiến trình 4nm tiên tiến lại đạt được mức IPC tăng 16%, giúp các tác vụ đơn nhân và gaming phản hồi tức thì.</p>" +
                                "<h3>2. Lời khuyên chọn mua</h3>" +
                                "<ul>" +
                                "<li><strong>Nếu bạn ưu tiên chơi game thuần túy & tiết kiệm điện:</strong> AMD Ryzen 7 9700X hoặc 7800X3D là sự lựa chọn không thể tuyệt vời hơn nhờ mức tiêu thụ điện thấp và nhiệt độ mát mẻ.</li>" +
                                "<li><strong>Nếu bạn làm công việc kết hợp (Dựng video, Render 3D, Premiere, After Effects):</strong> Intel Core i7-14700K / i9-14900K với số nhân luồng vượt trội cùng công nghệ QuickSync sẽ tối ưu tốc độ preview timeline mượt mà hơn.</li>" +
                                "</ul>")
                        .viewCount(980)
                        .status(NewsStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(3))
                        .build(),

                News.builder()
                        .title("Tư vấn cấu hình Build PC Gaming & Đồ họa tầm giá 20 - 25 triệu đồng: Cân mượt game AAA và thiết kế 4K")
                        .slug("tu-van-cau-hinh-pc-gaming-do-hoa-20-25-trieu")
                        .category(adviceCat)
                        .author(adminUser)
                        .thumbnailUrl("https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=1000&auto=format&fit=crop")
                        .summary("Bật mí danh sách linh kiện cân đối nhất phân khúc 20-25 triệu: CPU Intel Core i5-13400F, Card đồ họa RTX 4060 8GB, 32GB RAM DDR5 và nguồn 650W chuẩn Bronze.")
                        .content("<h2>Phân khúc 20 - 25 triệu: Cấu hình chuẩn cho mọi nhu cầu</h2>" +
                                "<p>Với ngân sách từ 20 đến 25 triệu đồng, người dùng hoàn toàn có thể xây dựng một bộ case PC thế hệ mới chạy nền tảng RAM DDR5, trang bị card đồ họa thế hệ RTX 40-Series để vừa chiến game mượt mà vừa phục vụ thiết kế đồ họa, dựng video ngắn TikTok, YouTube chuyên nghiệp.</p>" +
                                "<h3>1. Chi tiết cấu hình đề xuất (Complexus Pro-Gamer 2026)</h3>" +
                                "<ul>" +
                                "<li><strong>CPU:</strong> Intel Core i5-13400F (10 nhân, 16 luồng, Boost 4.6GHz) — ~4.890.000đ</li>" +
                                "<li><strong>Bo mạch chủ (Mainboard):</strong> ASUS TUF GAMING B760M-PLUS WIFI DDR5 — ~3.890.000đ</li>" +
                                "<li><strong>Bộ nhớ RAM:</strong> Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz — ~2.790.000đ</li>" +
                                "<li><strong>Card đồ họa (VGA):</strong> MSI GeForce RTX 4060 VENTUS 2X 8GB OC — ~8.490.000đ</li>" +
                                "<li><strong>Ổ cứng SSD:</strong> Kingston NV3 1TB PCIe 4.0 NVMe M.2 (Đọc 6000MB/s) — ~1.750.000đ</li>" +
                                "<li><strong>Nguồn máy tính (PSU):</strong> Deepcool PK650D 650W 80 Plus Bronze — ~1.250.000đ</li>" +
                                "<li><strong>Vỏ case:</strong> Montech AIR 100 ARGB (Kèm 4 quạt LED đồng bộ) — ~1.150.000đ</li>" +
                                "<li><strong>Tản nhiệt CPU:</strong> Thermalright Assassin X 120 Refined SE ARGB — ~490.000đ</li>" +
                                "</ul>" +
                                "<p><strong>👉 Tổng chi phí trọn bộ: ~24.700.000đ</strong> (Được tặng kèm gói vệ sinh máy tính trọn đời tại Complexus).</p>")
                        .viewCount(2150)
                        .status(NewsStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(5))
                        .build(),

                News.builder()
                        .title("Top 5 sai lầm tai hại khi tự lắp ráp máy tính (Build PC) tại nhà mà người mới nhất định phải tránh")
                        .slug("top-5-sai-lam-khi-tu-lap-rap-may-tinh-pc-tai-nha")
                        .category(guideCat)
                        .author(adminUser)
                        .thumbnailUrl("https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=1000&auto=format&fit=crop")
                        .summary("Quên bôi keo tản nhiệt, gắn sai khe RAM kênh đôi (Dual Channel), lắp quạt case ngược hướng lưu thông khí hay chọn nguồn công suất ảo khiến máy bị sập nguồn, quá nhiệt.")
                        .content("<h2>Tự ráp PC tại nhà: Dễ nhưng đừng chủ quan!</h2>" +
                                "<p>Tự tay lắp ráp một cỗ máy tính theo ý thích là trải nghiệm vô cùng thú vị của mọi game thủ và dân đam mê phần cứng. Tuy nhiên, nếu thiếu kiến thức căn bản, bạn rất dễ mắc phải những sai lầm có thể làm hỏng linh kiện đắt tiền.</p>" +
                                "<h3>1. Sai lầm #1: Quên bóc miếng nilon bảo vệ dưới đáy tản nhiệt CPU</h3>" +
                                "<p>Đây là \"căn bệnh quốc dân\" của ngay cả những người đã từng lắp máy vài lần. Dưới đáy của tản nhiệt khí hoặc tản nhiệt nước AIO luôn có một lớp nilon mỏng. Nếu quên bóc, CPU sẽ chạm ngưỡng 100 độ C ngay khi khởi động và tự ngắt.</p>" +
                                "<h3>2. Sai lầm #2: Cắm RAM sai khe kênh đôi (Dual Channel)</h3>" +
                                "<p>Trên các bo mạch chủ có 4 khe RAM, thứ tự cắm chuẩn cho kit 2 thanh RAM luôn là khe <strong>DIMM 2 và DIMM 4</strong> (tính từ phía CPU sang phải).</p>" +
                                "<h3>3. Sai lầm #3: Chọn nguồn công suất ảo</h3>" +
                                "<p>Sử dụng nguồn không đạt chứng nhận 80 Plus sẽ khiến dòng điện cấp cho VGA và CPU không ổn định, nguy cơ chập cháy linh kiện khi tải nặng rất cao.</p>")
                        .viewCount(3200)
                        .status(NewsStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(6))
                        .build(),

                News.builder()
                        .title("Đánh giá MacBook Pro M3 Max: \"Quái vật hiệu năng\" đồ họa di động, pin trâu vượt trội cho Creator")
                        .slug("danh-gia-macbook-pro-m3-max-quai-vat-hieu-nang")
                        .category(reviewCat)
                        .author(adminUser)
                        .thumbnailUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1000&auto=format&fit=crop")
                        .summary("Trải nghiệm thực tế sức mạnh của chip Apple Silicon M3 Max với 16 nhân CPU và 40 nhân GPU: Dựng phim 8K ProRes mượt mà, thời lượng pin liên tục hơn 18 tiếng.")
                        .content("<h2>MacBook Pro M3 Max: Đỉnh cao máy tính xách tay chuyên nghiệp</h2>" +
                                "<p>Dòng chip Apple M3 Max được sản xuất trên tiến trình 3 nanomet đột phá của TSMC, mang đến mật độ bóng bán dẫn khổng lồ cùng kiến trúc GPU thế hệ mới hỗ trợ phần cứng Ray Tracing ngay trên macOS.</p>" +
                                "<h3>1. Màn hình Liquid Retina XDR Mini-LED tuyệt mỹ</h3>" +
                                "<p>Độ sáng đạt đỉnh 1.600 nits khi phát HDR, độ bao phủ màu sắc đạt chuẩn 100% DCI-P3 cùng tần số quét 120Hz ProMotion.</p>" +
                                "<h3>2. Thời lượng pin phá vỡ mọi giới hạn</h3>" +
                                "<p>Cỗ máy duy trì hiệu năng 100% khi dùng pin mà không bị bóp xung. Thời lượng sử dụng thực tế đạt hơn 18 tiếng liên tục.</p>")
                        .viewCount(1670)
                        .status(NewsStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(8))
                        .build(),

                News.builder()
                        .title("Ổ cứng SSD PCIe 5.0 NVMe tốc độ 14.000 MB/s: Liệu người dùng phổ thông và Game thủ có thực sự cần?")
                        .slug("o-cung-ssd-pcie-5-0-nvme-toc-do-14000-mbs-co-can-thiet")
                        .category(techNewsCat)
                        .author(adminUser)
                        .thumbnailUrl("https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?q=80&w=1000&auto=format&fit=crop")
                        .summary("Khám phá chuẩn giao tiếp PCIe 5.0 x4 với băng thông gấp đôi PCIe 4.0. Đánh giá tốc độ load game DirectStorage, thời gian khởi động Windows và yêu cầu tản nhiệt tản đồng kèm quạt.")
                        .content("<h2>Sự bùng nổ của chuẩn lưu trữ PCIe Gen 5</h2>" +
                                "<p>Các mẫu ổ cứng SSD PCIe 5.0 NVMe M.2 mới nhất hiện nay như Crucial T700, Seagate FireCuda 540 hay Corsair MP700 PRO đã chính thức cán mốc tốc độ đọc ghi tuần tự lên đến 14.000 MB/s — nhanh gấp đôi so với PCIe 4.0.</p>" +
                                "<h3>Lời khuyên đầu tư hợp lý</h3>" +
                                "<ul>" +
                                "<li><strong>Với người dùng cá nhân & Game thủ:</strong> Một chiếc SSD PCIe 4.0 (như Kingston KC3000, Samsung 990 Pro) với dung lượng 1TB - 2TB là lựa chọn tối ưu nhất về cả chi phí lẫn nhiệt độ mát mẻ.</li>" +
                                "<li><strong>Với chuyên gia đồ họa & AI:</strong> SSD PCIe 5.0 phát huy sức mạnh tối đa khi cần copy các tệp dữ liệu 8K RAW hàng trăm GB hoặc nạp mô hình AI lớn.</li>" +
                                "</ul>")
                        .viewCount(850)
                        .status(NewsStatus.PUBLISHED)
                        .publishedAt(LocalDateTime.now().minusDays(10))
                        .build()
        );

        newsRepository.saveAll(initialArticles);
        log.info("Successfully seeded {} tech news articles.", initialArticles.size());
    }
}
