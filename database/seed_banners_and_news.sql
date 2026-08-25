-- ==============================================================================
-- COMPLEXUS - SEED DATA FOR BANNERS & TECH NEWS CMS
-- Dữ liệu mẫu Banner động và Tin tức công nghệ được tổng hợp từ các chuyên trang:
-- CellphoneS (Sforum), Nguyễn Công PC, Thế Giới Di Động (24h Công Nghệ), Hoàng Hà Mobile, Điện Máy Xanh
-- ==============================================================================

USE `computer_store_db`;

-- ------------------------------------------------------------------------------
-- 1. BANNERS (Slider Trang Chủ, Thanh Bên, Đầu Danh Mục)
-- ------------------------------------------------------------------------------
DELETE FROM `banners`;

INSERT INTO `banners` (`banner_id`, `title`, `image_url`, `link_url`, `position`, `sort_order`, `start_date`, `end_date`, `status`, `created_at`, `updated_at`) VALUES
(1, 'SIÊU HỘI LAPTOP GAMING & AI RTX 40-SERIES', 'https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=1600&auto=format&fit=crop', '/products?category=laptop-gaming', 'homepage_slider', 1, '2026-01-01 00:00:00', '2028-12-31 23:59:59', 'active', NOW(), NOW()),
(2, 'BUILD PC WORKSTATION & GAMING CAO CẤP - TẶNG TẢN NHIỆT AIO 360', 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?q=80&w=1600&auto=format&fit=crop', '/products?category=pc-gaming-streamer', 'homepage_slider', 2, '2026-01-01 00:00:00', '2028-12-31 23:59:59', 'active', NOW(), NOW()),
(3, 'APPLE MACBOOK M3 SERIES CHÍNH HÃNG - TRỢ GIÁ ĐỔI CŨ LẤY MỚI', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1600&auto=format&fit=crop', '/products?category=macbook-apple', 'homepage_slider', 3, '2026-01-01 00:00:00', '2028-12-31 23:59:59', 'active', NOW(), NOW()),
(4, 'TUẦN LỄ LINH KIỆN PC: VGA RTX 4070 SUPER, RAM DDR5, SSD GEN 4 GIẢM 35%', 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=1600&auto=format&fit=crop', '/products?category=linh-kien-may-tinh', 'homepage_slider', 4, '2026-01-01 00:00:00', '2028-12-31 23:59:59', 'active', NOW(), NOW()),
(5, 'MÀN HÌNH GAMING FAST IPS 2K 240HZ - TRẢI NGHIỆM ĐỒ HỌA ĐỈNH CAO', 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?q=80&w=1000&auto=format&fit=crop', '/products?category=man-hinh-may-tinh', 'sidebar', 1, '2026-01-01 00:00:00', '2028-12-31 23:59:59', 'active', NOW(), NOW());

-- ------------------------------------------------------------------------------
-- 2. NEWS CATEGORIES (Danh mục Tin Tức & Bài Viết)
-- ------------------------------------------------------------------------------
DELETE FROM `news`;
DELETE FROM `news_categories`;

INSERT INTO `news_categories` (`news_cat_id`, `name`, `slug`, `description`, `sort_order`, `status`, `created_at`, `updated_at`) VALUES
(1, 'Đánh giá & Review', 'danh-gia-review', 'Chuyên mục đánh giá chi tiết phần cứng, laptop, linh kiện PC và phụ kiện công nghệ thực tế', 1, 'active', NOW(), NOW()),
(2, 'Thủ thuật & Hướng dẫn', 'thu-thuat-huong-dan', 'Hướng dẫn lắp ráp máy tính (Build PC), tối ưu Windows, khắc phục lỗi phần cứng và phần mềm', 2, 'active', NOW(), NOW()),
(3, 'Tin tức Công nghệ', 'tin-tuc-cong-nghe', 'Cập nhật xu hướng vi xử lý, card đồ họa, AI, bộ nhớ và các sự kiện công nghệ toàn cầu', 3, 'active', NOW(), NOW()),
(4, 'Tư vấn Cấu hình', 'tu-van-cau-hinh', 'Tư vấn lựa chọn cấu hình máy tính gaming, làm đồ họa, workstation phù hợp từng mức ngân sách', 4, 'active', NOW(), NOW());

-- ------------------------------------------------------------------------------
-- 3. NEWS ARTICLES (Bài viết công nghệ chất lượng cao)
-- ------------------------------------------------------------------------------
INSERT INTO `news` (`news_id`, `title`, `slug`, `news_cat_id`, `user_id`, `thumbnail_url`, `summary`, `content`, `view_count`, `status`, `published_at`, `created_at`, `updated_at`) VALUES
(1,
 'Đánh giá chi tiết NVIDIA GeForce RTX 4070 Super: "Ông vua" đồ họa 2K và Gaming đỉnh cao 2026',
 'danh-gia-nvidia-geforce-rtx-4070-super-ong-vua-do-hoa-2k',
 1,
 1,
 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?q=80&w=1000&auto=format&fit=crop',
 'GeForce RTX 4070 Super mang đến bước nhảy vọt về hiệu năng nhờ kiến trúc Ada Lovelace, 12GB VRAM GDDR6X cùng DLSS 3.5 Frame Generation, cân mượt mọi tựa game AAA ở độ phân giải 2K 144Hz.',
 '<h2>Tổng quan về NVIDIA GeForce RTX 4070 Super</h2>
<p>Kể từ khi ra mắt, dòng sản phẩm RTX 40-Series Super của NVIDIA đã nhanh chóng định hình lại phân khúc card màn hình tầm trung - cận cao cấp. Trong đó, <strong>RTX 4070 Super</strong> là cái tên nổi bật nhất nhờ mức hiệu năng tiệm cận với RTX 4070 Ti nhưng sở hữu mức giá hấp dẫn hơn đáng kể.</p>

<h3>1. Thông số kỹ thuật ấn tượng</h3>
<ul>
  <li><strong>Số nhân CUDA:</strong> 7.168 nhân (tăng gần 22% so với 5.888 nhân trên bản 4070 tiêu chuẩn).</li>
  <li><strong>Xung nhịp Boost:</strong> Lên đến 2.475 MHz.</li>
  <li><strong>Bộ nhớ VRAM:</strong> 12GB GDDR6X tốc độ 21 Gbps, bus 192-bit.</li>
  <li><strong>Mức tiêu thụ điện năng (TDP):</strong> Chỉ 220W, cực kỳ tiết kiệm điện năng nhờ tiến trình 4N TSMC.</li>
</ul>

<h3>2. Hiệu năng Gaming thực tế ở độ phân giải 2K (1440p)</h3>
<p>Thử nghiệm trên hệ thống CPU Core i7-14700K và 32GB RAM DDR5 6000MHz cho thấy kết quả ấn tượng:</p>
<ul>
  <li><strong>Cyberpunk 2077 (Ray Tracing Overdrive, DLSS 3.5 Quality):</strong> 95 - 110 FPS vô cùng mượt mà.</li>
  <li><strong>Black Myth: Wukong (Cinematic Setting, Full Ray Tracing):</strong> Đạt trung bình 82 FPS ở độ phân giải 2K.</li>
  <li><strong>Counter-Strike 2 & Valorant:</strong> Luôn duy trì trên 400 FPS, khai thác tối đa màn hình tần số quét cao 240Hz/360Hz.</li>
</ul>

<h3>3. Khả năng làm việc đồ họa chuyên nghiệp</h3>
<p>Với bộ nhớ 12GB VRAM chuẩn GDDR6X kết hợp cùng các nhân RT Core Gen 3 và Tensor Core Gen 4, RTX 4070 Super xử lý xuất sắc các tác vụ render 3D trên Blender, biên tập timeline 4K/6K trên Adobe Premiere Pro và DaVinci Resolve Studio với thời gian xuất file nhanh hơn 35% so với thế hệ RTX 3070 trước đây.</p>

<h3>4. Kết luận</h3>
<p>NVIDIA GeForce RTX 4070 Super xứng đáng là lựa chọn hàng đầu cho các bộ PC Gaming & Workstation tầm trung - cao cấp hiện nay. Sản phẩm hiện đang được phân phối chính hãng tại hệ thống Complexus với chế độ bảo hành 36 tháng 1 đổi 1.</p>',
 1420,
 'published',
 NOW(),
 NOW(),
 NOW()),

(2,
 'So sánh Intel Core Gen 14th vs AMD Ryzen 9000 Series: Đâu là CPU tối ưu nhất cho Gaming và Render?',
 'so-sanh-intel-gen-14-vs-amd-ryzen-9000-series',
 3,
 1,
 'https://images.unsplash.com/photo-1555680202-c86f0e12f086?q=80&w=1000&auto=format&fit=crop',
 'Cuộc chạm trán nảy lửa giữa kiến trúc Raptor Lake Refresh của Intel và Zen 5 của AMD. Phân tích chi tiết mức tiêu thụ điện năng, nhiệt độ và hiệu năng đơn nhân/đa nhân thực tế.',
 '<h2>Cuộc đua vi xử lý năm 2026: Intel hay AMD?</h2>
<p>Thị trường CPU máy tính để bàn chứng kiến sự cạnh tranh quyết liệt giữa dòng chip <strong>Intel Core Gen 14th (Raptor Lake Refresh)</strong> và đối thủ truyền kiếp <strong>AMD Ryzen 9000 Series (Kiến trúc Zen 5)</strong>. Cả hai đều mang đến những cải tiến vượt bậc về IPC và xung nhịp.</p>

<h3>1. Kiến trúc và hiệu năng IPC</h3>
<p>Trong khi Intel tiếp tục hoàn thiện kiến trúc lai kết hợp giữa Performance Cores (P-Cores) và Efficient Cores (E-Cores) mang lại khả năng đa nhiệm tuyệt vời cho các phần mềm dựng hình đa luồng, thì AMD với tiến trình 4nm tiên tiến lại đạt được mức IPC tăng 16%, giúp các tác vụ đơn nhân và gaming phản hồi tức thì.</p>

<h3>2. Bảng so sánh Benchmark Cinebench R23</h3>
<table border="1" cellpadding="8" style="width: 100%; border-collapse: collapse; margin: 16px 0;">
  <tr style="background: #f1f5f9;">
    <th>Vi xử lý (CPU)</th>
    <th>Điểm Đơn Nhân (Single-Core)</th>
    <th>Điểm Đa Nhân (Multi-Core)</th>
    <th>TDP Thực Tế</th>
  </tr>
  <tr>
    <td><strong>Intel Core i7-14700K</strong></td>
    <td>2.180 pts</td>
    <td>35.400 pts</td>
    <td>253W</td>
  </tr>
  <tr>
    <td><strong>AMD Ryzen 7 9700X</strong></td>
    <td>2.240 pts</td>
    <td>23.800 pts</td>
    <td>88W</td>
  </tr>
  <tr>
    <td><strong>Intel Core i9-14900K</strong></td>
    <td>2.310 pts</td>
    <td>40.900 pts</td>
    <td>320W</td>
  </tr>
  <tr>
    <td><strong>AMD Ryzen 9 9950X</strong></td>
    <td>2.290 pts</td>
    <td>42.100 pts</td>
    <td>200W</td>
  </tr>
</table>

<h3>3. Lời khuyên chọn mua</h3>
<ul>
  <li><strong>Nếu bạn ưu tiên chơi game thuần túy & tiết kiệm điện:</strong> AMD Ryzen 7 9700X hoặc 7800X3D là sự lựa chọn không thể tuyệt vời hơn nhờ mức tiêu thụ điện thấp và nhiệt độ mát mẻ.</li>
  <li><strong>Nếu bạn làm công việc kết hợp (Dựng video, Render 3D, Premiere, After Effects):</strong> Intel Core i7-14700K / i9-14900K với số nhân luồng vượt trội cùng công nghệ QuickSync sẽ tối ưu tốc độ preview timeline mượt mà hơn.</li>
</ul>',
 980,
 'published',
 NOW(),
 NOW(),
 NOW()),

(3,
 'Tư vấn cấu hình Build PC Gaming & Đồ họa tầm giá 20 - 25 triệu đồng: Cân mượt game AAA và thiết kế 4K',
 'tu-van-cau-hinh-pc-gaming-do-hoa-20-25-trieu',
 4,
 1,
 'https://images.unsplash.com/photo-1593640408182-31c70c8268f5?q=80&w=1000&auto=format&fit=crop',
 'Bật mí danh sách linh kiện cân đối nhất phân khúc 20-25 triệu: CPU Intel Core i5-13400F, Card đồ họa RTX 4060 8GB, 32GB RAM DDR5 và nguồn 650W chuẩn Bronze.',
 '<h2>Phân khúc 20 - 25 triệu: Cấu hình chuẩn cho mọi nhu cầu</h2>
<p>Với ngân sách từ 20 đến 25 triệu đồng, người dùng hoàn toàn có thể xây dựng một bộ case PC thế hệ mới chạy nền tảng RAM DDR5, trang bị card đồ họa thế hệ RTX 40-Series để vừa chiến game mượt mà vừa phục vụ thiết kế đồ họa, dựng video ngắn TikTok, YouTube chuyên nghiệp.</p>

<h3>1. Chi tiết cấu hình đề xuất (Complexus Pro-Gamer 2026)</h3>
<ul>
  <li><strong>CPU:</strong> Intel Core i5-13400F (10 nhân, 16 luồng, Boost 4.6GHz) — ~4.890.000đ</li>
  <li><strong>Bo mạch chủ (Mainboard):</strong> ASUS TUF GAMING B760M-PLUS WIFI DDR5 — ~3.890.000đ</li>
  <li><strong>Bộ nhớ RAM:</strong> Corsair Vengeance RGB 32GB (2x16GB) DDR5 6000MHz — ~2.790.000đ</li>
  <li><strong>Card đồ họa (VGA):</strong> MSI GeForce RTX 4060 VENTUS 2X 8GB OC — ~8.490.000đ</li>
  <li><strong>Ổ cứng SSD:</strong> Kingston NV3 1TB PCIe 4.0 NVMe M.2 (Đọc 6000MB/s) — ~1.750.000đ</li>
  <li><strong>Nguồn máy tính (PSU):</strong> Deepcool PK650D 650W 80 Plus Bronze — ~1.250.000đ</li>
  <li><strong>Vỏ case:</strong> Montech AIR 100 ARGB (Kèm 4 quạt LED đồng bộ) — ~1.150.000đ</li>
  <li><strong>Tản nhiệt CPU:</strong> Thermalright Assassin X 120 Refined SE ARGB — ~490.000đ</li>
</ul>

<p><strong>👉 Tổng chi phí trọn bộ: ~24.700.000đ</strong> (Được tặng kèm gói vệ sinh máy tính trọn đời tại Complexus).</p>

<h3>2. Khả năng đáp ứng thực tế</h3>
<p>Bộ máy chiến tốt mọi tựa game Esport ở mức 200+ FPS, cân mượt GTA V, God of War, Forza Horizon 5 ở thiết lập đồ họa Ultra Setting. Ngoài ra, dung lượng 32GB RAM DDR5 đảm bảo bạn mở cùng lúc Photoshop, Illustrator, Premiere Pro và hàng chục tab Chrome mà không hề có hiện tượng tràn bộ nhớ hay giật lag.</p>',
 2150,
 'published',
 NOW(),
 NOW(),
 NOW()),

(4,
 'Top 5 sai lầm tai hại khi tự lắp ráp máy tính (Build PC) tại nhà mà người mới nhất định phải tránh',
 'top-5-sai-lam-khi-tu-lap-rap-may-tinh-pc-tai-nha',
 2,
 1,
 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=1000&auto=format&fit=crop',
 'Quên bôi keo tản nhiệt, gắn sai khe RAM kênh đôi (Dual Channel), lắp quạt case ngược hướng lưu thông khí hay chọn nguồn công suất ảo khiến máy bị sập nguồn, quá nhiệt.',
 '<h2>Tự ráp PC tại nhà: Dễ nhưng đừng chủ quan!</h2>
<p>Tự tay lắp ráp một cỗ máy tính theo ý thích là trải nghiệm vô cùng thú vị của mọi game thủ và dân đam mê phần cứng. Tuy nhiên, nếu thiếu kiến thức căn bản, bạn rất dễ mắc phải những sai lầm có thể làm hỏng linh kiện đắt tiền.</p>

<h3>1. Sai lầm #1: Quên bóc miếng nilon bảo vệ dưới đáy tản nhiệt CPU</h3>
<p>Đây là "căn bệnh quốc dân" của ngay cả những người đã từng lắp máy vài lần. Dưới đáy của tản nhiệt khí hoặc tản nhiệt nước AIO luôn có một lớp nilon mỏng in chữ <em>"Please peel off before installation"</em>. Nếu quên bóc miếng dán này, CPU sẽ chạm ngưỡng 100 độ C ngay khi khởi động và tự sập để bảo vệ quá nhiệt (Thermal Throttling).</p>

<h3>2. Sai lầm #2: Cắm RAM sai khe kênh đôi (Dual Channel)</h3>
<p>Trên các bo mạch chủ có 4 khe RAM, thứ tự cắm chuẩn cho kit 2 thanh RAM luôn là khe <strong>DIMM 2 và DIMM 4</strong> (tính từ phía CPU sang phải). Cắm sai vị trí (ví dụ khe 1 và 2) sẽ khiến máy chỉ chạy ở chế độ Single Channel, làm giảm từ 15 - 25% hiệu năng xử lý trong game.</p>

<h3>3. Sai lầm #3: Chọn nguồn công suất ảo (Noname, chất lượng kém)</h3>
<p>Nguồn máy tính (PSU) được ví như trái tim của toàn bộ hệ thống. Sử dụng những bộ nguồn trôi nổi, không rõ nguồn gốc hoặc không đạt chứng nhận 80 Plus sẽ khiến dòng điện cấp cho card đồ họa và CPU không ổn định, nguy cơ gây cháy nổ linh kiện khi tải nặng là rất cao.</p>

<h3>4. Sai lầm #4: Quạt case lắp ngược hướng luồng gió</h3>
<p>Nguyên lý luồng khí chuẩn (Airflow) là: <strong>Mặt trước hút gió mát vào, mặt sau và mặt nóc thổi khí nóng ra</strong>. Nhiều bạn mới lắp đã gắn tất cả các quạt theo hướng thổi ra hoặc hút vào, tạo áp suất bất đối xứng khiến bụi bẩn bị hút vào trong và nhiệt độ case tăng vọt.</p>

<h3>5. Sai lầm #5: Lắp chặn main (I/O Shield) sau khi đã cố định Mainboard</h3>
<p>Cố định xong ốc vít mainboard, tản nhiệt, dây nguồn rồi mới nhận ra quên gắn miếng chặn cổng kết nối I/O Shield phía sau là tình huống "dở khóc dở cười" buộc bạn phải tháo toàn bộ bo mạch chủ ra để làm lại từ đầu.</p>',
 3200,
 'published',
 NOW(),
 NOW(),
 NOW()),

(5,
 'Đánh giá MacBook Pro M3 Max: "Quái vật hiệu năng" đồ họa di động, pin trâu vượt trội cho Creator',
 'danh-gia-macbook-pro-m3-max-quai-vat-hieu-nang',
 1,
 1,
 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=1000&auto=format&fit=crop',
 'Trải nghiệm thực tế sức mạnh của chip Apple Silicon M3 Max với 16 nhân CPU và 40 nhân GPU: Dựng phim 8K ProRes mượt mà, thời lượng pin liên tục hơn 18 tiếng.',
 '<h2>MacBook Pro M3 Max: Đỉnh cao máy tính xách tay chuyên nghiệp</h2>
<p>Dòng chip Apple M3 Max được sản xuất trên tiến trình 3 nanomet đột phá của TSMC, mang đến mật độ bóng bán dẫn khổng lồ cùng kiến trúc GPU thế hệ mới hỗ trợ phần cứng Ray Tracing và Mesh Shading ngay trên macOS.</p>

<h3>1. Màn hình Liquid Retina XDR Mini-LED tuyệt mỹ</h3>
<p>Màn hình của MacBook Pro tiếp tục giữ vị thế số 1 thế giới laptop với độ sáng duy trì 1.000 nits và đạt đỉnh 1.600 nits khi phát nội dung HDR. Độ bao phủ màu sắc đạt chuẩn 100% DCI-P3 cùng công nghệ ProMotion 120Hz mang đến độ mượt mà tuyệt đối khi cuộn trang hay chỉnh sửa timeline.</p>

<h3>2. Hiệu năng render thực tế với chip M3 Max (16 CPU / 40 GPU)</h3>
<ul>
  <li><strong>Xuất video 4K ProRes 10-bit (Thời lượng 20 phút):</strong> Chỉ mất 2 phút 15 giây trên Final Cut Pro.</li>
  <li><strong>Dựng hình 3D trên Blender:</strong> Khả năng tính toán Ray Tracing bằng phần cứng giúp thời gian render giảm 2.5 lần so với M2 Max.</li>
  <li><strong>Lập trình Xcode:</strong> Biên dịch dự án iOS quy mô hàng triệu dòng code trong chưa đầy 45 giây.</li>
</ul>

<h3>3. Thời lượng pin phá vỡ mọi giới hạn</h3>
<p>Điểm đáng kinh ngạc nhất là cỗ máy này vẫn duy trì hiệu năng 100% khi dùng pin mà không bị bóp xung như các dòng laptop Windows. Thời lượng sử dụng thực tế cho các tác vụ văn phòng, duyệt web và xem video đạt hơn 18 tiếng liên tục.</p>',
 1670,
 'published',
 NOW(),
 NOW(),
 NOW()),

(6,
 'Ổ cứng SSD PCIe 5.0 NVMe tốc độ 14.000 MB/s: Liệu người dùng phổ thông và Game thủ có thực sự cần?',
 'o-cung-ssd-pcie-5-0-nvme-toc-do-14000-mbs-co-can-thiet',
 3,
 1,
 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?q=80&w=1000&auto=format&fit=crop',
 'Khám phá chuẩn giao tiếp PCIe 5.0 x4 với băng thông gấp đôi PCIe 4.0. Đánh giá tốc độ load game DirectStorage, thời gian khởi động Windows và yêu cầu tản nhiệt tản đồng kèm quạt.',
 '<h2>Sự bùng nổ của chuẩn lưu trữ PCIe Gen 5</h2>
<p>Các mẫu ổ cứng SSD PCIe 5.0 NVMe M.2 mới nhất hiện nay như Crucial T700, Seagate FireCuda 540 hay Corsair MP700 PRO đã chính thức cán mốc tốc độ đọc ghi tuần tự lên đến 14.000 MB/s — nhanh gấp đôi so với các dòng SSD PCIe 4.0 cao cấp nhất (7.000 MB/s).</p>

<h3>1. Sự khác biệt giữa tốc độ tuần tự và 4K Random</h3>
<p>Mặc dù con số 14.000 MB/s trông rất hào nhoáng, phần lớn các tác vụ hàng ngày như khởi động Windows, mở ứng dụng hay tải màn chơi trong game lại phụ thuộc chủ yếu vào tốc độ đọc ghi ngẫu nhiên (4K Random IOPS). Ở tác vụ này, SSD Gen 5 chỉ nhanh hơn Gen 4 khoảng 15 - 20%.</p>

<h3>2. Thách thức về nhiệt độ hoạt động</h3>
<p>Bộ điều khiển (Controller) của SSD PCIe 5.0 tiêu thụ công suất điện khá lớn và tỏa ra lượng nhiệt rất cao. Hầu hết các mẫu SSD Gen 5 đều bắt buộc phải đi kèm khối tản nhiệt nhôm cỡ lớn có kèm quạt tản nhiệt mini hoặc kết nối trực tiếp vào tản nhiệt nước custom để tránh bị quá nhiệt hạ xung (Thermal Throttling).</p>

<h3>3. Lời khuyên đầu tư hợp lý</h3>
<ul>
  <li><strong>Với người dùng cá nhân & Game thủ:</strong> Một chiếc SSD PCIe 4.0 (như Kingston KC3000, Samsung 990 Pro) với dung lượng 1TB - 2TB là lựa chọn tối ưu nhất về cả chi phí lẫn nhiệt độ mát mẻ.</li>
  <li><strong>Với chuyên gia đồ họa & AI / Server:</strong> SSD PCIe 5.0 phát huy sức mạnh tối đa khi cần copy, di chuyển các tệp dữ liệu video 8K RAW hàng trăm Gigabyte hoặc nạp các mô hình dữ liệu AI khổng lồ vào bộ nhớ.</li>
</ul>',
 850,
 'published',
 NOW(),
 NOW(),
 NOW());
