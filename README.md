# Computer & PC Components E-commerce Platform

Nền tảng thương mại điện tử bán máy tính và linh kiện PC, gồm website mua hàng dành cho khách và hệ thống quản trị vận hành. Dự án được tổ chức theo mô hình monorepo với backend Spring Boot, frontend Angular, MySQL làm cơ sở dữ liệu chính và Redis cho cache/rate limiting.

> Trạng thái hiện tại: release candidate/hardening. Dự án đã có test tự động và CI, nhưng trước khi triển khai production cần hoàn thành checklist tại mục [Production checklist](#production-checklist).

## Mục lục

- [Tính năng chính](#tính-năng-chính)
- [Kiến trúc và công nghệ](#kiến-trúc-và-công-nghệ)
- [Cấu trúc repository](#cấu-trúc-repository)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Khởi chạy nhanh](#khởi-chạy-nhanh)
- [Cấu hình môi trường](#cấu-hình-môi-trường)
- [Cơ sở dữ liệu và Flyway](#cơ-sở-dữ-liệu-và-flyway)
- [Chạy backend](#chạy-backend)
- [Chạy frontend](#chạy-frontend)
- [API và phân quyền](#api-và-phân-quyền)
- [Redis và cache](#redis-và-cache)
- [Upload và tài nguyên tĩnh](#upload-và-tài-nguyên-tĩnh)
- [Kiểm thử](#kiểm-thử)
- [CI/CD](#cicd)
- [Triển khai production](#triển-khai-production)
- [Production checklist](#production-checklist)
- [Xử lý sự cố](#xử-lý-sự-cố)
- [Quy ước phát triển](#quy-ước-phát-triển)

## Tính năng chính

### Storefront

- Xem danh mục, thương hiệu, danh sách và chi tiết sản phẩm.
- Biến thể, hình ảnh và thông số kỹ thuật động theo danh mục.
- Tìm kiếm/lọc sản phẩm, xem tồn kho khả dụng.
- Giỏ hàng cho khách và người dùng đã đăng nhập; hỗ trợ merge giỏ hàng.
- Checkout, xem lịch sử đơn và hủy đơn theo quy tắc nghiệp vụ.
- Guest checkout và tra cứu đơn bằng mã đơn kết hợp số điện thoại.
- Mã giảm giá, đánh giá sản phẩm, tin tức và banner.
- Quản lý hồ sơ, địa chỉ và đổi mật khẩu.
- Yêu cầu đổi/trả hàng.
- Chat hỗ trợ qua STOMP/SockJS, gồm khách chưa đăng nhập.
- Đăng nhập email/mật khẩu và social login Google, Facebook, Zalo.

### Admin panel

- Dashboard và thống kê doanh thu, trạng thái đơn, sản phẩm bán chạy.
- Quản lý danh mục, thuộc tính, thương hiệu, sản phẩm, biến thể và hình ảnh.
- Quản lý đơn hàng, trạng thái thanh toán và đổi/trả.
- Quản lý kho, điều chỉnh tồn, nhập kho, chuyển kho và lịch sử kho.
- Quản lý nhà cung cấp, mã giảm giá, banner, tin tức và đánh giá.
- Quản lý khách hàng, nhân viên, vai trò và quyền hạn.
- Quản lý chat, hội thoại và luật trả lời tự động.
- Cấu hình website và xem/xuất audit log.

### Bảo mật và vận hành

- Spring Security stateless với JWT access token và refresh token.
- BCrypt cho mật khẩu; phân quyền theo role/permission ở backend.
- Rate limiting đăng nhập bằng Redis.
- CORS cấu hình theo danh sách origin.
- Trusted proxy/forwarded headers cho môi trường reverse proxy.
- Flyway quản lý phiên bản schema và dữ liệu tham chiếu.
- Actuator health check; Swagger chỉ bật trong profile development.
- CI riêng cho backend và frontend.

## Kiến trúc và công nghệ

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Spring MVC |
| Security | Spring Security, JWT/JJWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8.x |
| Migration | Flyway |
| Cache | Redis 7.x, Spring Cache, Lettuce |
| Realtime | WebSocket, STOMP, SockJS |
| API docs | OpenAPI 3, springdoc-openapi |
| Frontend | Angular 21, TypeScript 5.9, RxJS 7.8 |
| Frontend tests | Vitest + jsdom |
| Backend tests | JUnit 5, Mockito, Spring Security Test |
| Build | Maven Wrapper và npm |
| CI | GitHub Actions |

Luồng xử lý backend tuân theo kiến trúc phân lớp:

```text
HTTP request
    -> Controller
    -> Service interface
    -> Service implementation
    -> Repository
    -> MySQL
```

Các thành phần dùng chung như JWT filter, exception handler, cache, audit và validation được xử lý xuyên suốt các lớp. Frontend chia riêng route tree và layout của storefront/admin; backend vẫn là nơi quyết định quyền truy cập thực sự.

## Cấu trúc repository

```text
java_angular/
├── .github/workflows/        # Backend CI và Frontend CI
├── database/                 # SQL tham khảo/legacy và các script dữ liệu hỗ trợ
├── demo/                     # Backend Spring Boot
│   ├── src/main/java/com/store/
│   │   ├── config/           # Security, CORS, Redis, WebSocket, OpenAPI
│   │   ├── controller/       # REST API
│   │   ├── dto/              # Request/response DTO
│   │   ├── entity/           # JPA entities
│   │   ├── repository/       # Spring Data repositories
│   │   ├── security/         # JWT, entry point, rate limiter
│   │   ├── service/          # Service interfaces/implementations
│   │   └── util/             # Tiện ích dùng chung
│   ├── src/main/resources/
│   │   ├── db/migration/     # Flyway migrations
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   └── src/test/             # Unit/integration tests
├── frontend/                 # Angular application
│   ├── public/               # Assets được phục vụ trực tiếp
│   └── src/app/
│       ├── core/             # Guards, interceptors, models, services
│       ├── features/         # Auth, shop và admin
│       ├── layout/           # Public shell và admin shell
│       └── shared/           # Component dùng chung
├── uploads/                  # Dữ liệu upload local; không commit Git
├── docs/                     # Tài liệu chuyên đề
├── .env.example              # Danh sách biến môi trường mẫu
└── AGENTS.md                 # Quy ước bắt buộc khi sửa code bằng agent
```

## Yêu cầu môi trường

Cài đặt các công cụ sau:

- JDK 21.
- MySQL 8.x.
- Redis 7.x.
- Node.js 22 LTS và npm 10.x.
- Git.

Không cần cài Maven toàn hệ thống vì repository có Maven Wrapper (`mvnw`/`mvnw.cmd`).

Kiểm tra phiên bản:

```powershell
java -version
node --version
npm --version
mysql --version
redis-cli --version
```

## Khởi chạy nhanh

### 1. Clone và chuẩn bị biến môi trường

```powershell
git clone <repository-url> java_angular
cd java_angular
Copy-Item .env.example .env
```

File `.env` đã được Git ignore, nhưng Spring Boot không tự động đọc file này khi chạy trực tiếp. Hãy khai báo các biến trong IDE, terminal, service manager hoặc hệ thống secret của môi trường triển khai.

Ví dụ cho phiên PowerShell hiện tại:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "computer_store_db"
$env:DB_USERNAME = "store_app"
$env:DB_PASSWORD = "your-local-password"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:JWT_SECRET = "replace-with-a-long-random-secret"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:4200,http://127.0.0.1:4200"
```

### 2. Tạo database và tài khoản local

Đăng nhập MySQL bằng tài khoản có quyền quản trị, sau đó chạy:

```sql
CREATE DATABASE computer_store_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'store_app'@'localhost' IDENTIFIED BY 'your-local-password';
GRANT ALL PRIVILEGES ON computer_store_db.* TO 'store_app'@'localhost';
FLUSH PRIVILEGES;
```

Không import thủ công `V1`/`V2`. Flyway sẽ tự chạy migration khi backend khởi động.

### 3. Khởi động Redis

Đảm bảo Redis đang lắng nghe ở `localhost:6379` hoặc cập nhật `REDIS_HOST`/`REDIS_PORT`.

Kiểm tra:

```powershell
redis-cli ping
```

Kết quả mong đợi:

```text
PONG
```

### 4. Chạy backend

```powershell
cd demo
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Backend mặc định chạy tại `http://localhost:8080`.

### 5. Chạy frontend

Mở terminal khác:

```powershell
cd frontend
npm ci
npm start
```

Frontend development chạy tại `http://localhost:4200`.

### 6. Kiểm tra hệ thống

- Storefront: `http://localhost:4200`
- Admin login: `http://localhost:4200/admin/login`
- Backend health: `http://localhost:8080/actuator/health`
- Swagger UI trong profile `dev`: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON trong profile `dev`: `http://localhost:8080/v3/api-docs`

## Cấu hình môi trường

### Biến bắt buộc hoặc quan trọng

| Biến | Mặc định local | Ý nghĩa |
|---|---|---|
| `SERVER_PORT` | `8080` | Cổng backend |
| `SPRING_PROFILES_ACTIVE` | không có | Dùng `dev` khi phát triển, `prod` khi production |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `computer_store_db` | Tên database |
| `DB_USERNAME` | `root` | MySQL username |
| `DB_PASSWORD` | rỗng | MySQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_TIMEOUT` | `3000ms` | Redis timeout |
| `JWT_SECRET` | không có ở runtime chính | Secret ký JWT; bắt buộc cung cấp |
| `JWT_ACCESS_EXPIRATION_MS` | `1800000` | Access token, mặc định 30 phút |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh token, mặc định 7 ngày |
| `CORS_ALLOWED_ORIGINS` | hai origin local | Danh sách origin, phân cách bằng dấu phẩy |

### Email

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | rỗng | SMTP username |
| `MAIL_PASSWORD` | rỗng | SMTP password/app password |
| `MAIL_FROM_ADDRESS` | `no-reply@complexus.vn` | Địa chỉ người gửi |
| `MAIL_FROM_NAME` | `Complexus Shop` | Tên hiển thị người gửi |

Trong profile `dev`, mail mặc định trỏ tới `localhost:1025`, phù hợp với SMTP catcher như Mailpit/MailHog.

### Social login

| Biến | Nhà cung cấp |
|---|---|
| `GOOGLE_CLIENT_ID` | Google |
| `FACEBOOK_APP_ID` | Facebook |
| `FACEBOOK_APP_SECRET` | Facebook |
| `ZALO_APP_ID` | Zalo |
| `ZALO_SECRET_KEY` | Zalo |

Nếu không cấu hình provider, không nên hiển thị hoặc sử dụng nút đăng nhập tương ứng. Zalo OAuth có walkthrough riêng tại [`docs/zalo-oauth-walkthrough.md`](docs/zalo-oauth-walkthrough.md).

### Quy tắc secret

- Không commit `.env`, JWT secret, DB password, SMTP password hoặc OAuth secret.
- Production phải dùng secret manager hoặc biến môi trường của nền tảng deploy.
- `JWT_SECRET` phải đủ dài, sinh ngẫu nhiên và khác giữa dev/staging/production.
- Không tái sử dụng credentials của MySQL root cho ứng dụng production.
- Rotate secret ngay nếu từng xuất hiện trong Git, log hoặc ảnh chụp màn hình.

## Cơ sở dữ liệu và Flyway

Flyway không phải công cụ backup. Nó quản lý lịch sử thay đổi schema/dữ liệu thông qua các migration có version.

Các migration hiện có:

```text
demo/src/main/resources/db/migration/
├── V1__current_schema.sql
└── V2__reference_data.sql
```

Khi backend khởi động:

1. Flyway đọc bảng `flyway_schema_history`.
2. Chạy các migration chưa được áp dụng theo thứ tự version.
3. Kiểm tra checksum của migration đã chạy.
4. Hibernate dùng `ddl-auto=validate` để xác nhận entity phù hợp schema; Hibernate không tự sửa schema.

### Quy tắc migration

- Mọi thay đổi schema mới phải nằm trong migration mới, ví dụ `V3__add_order_index.sql`.
- Không chỉnh sửa hoặc xóa migration đã áp dụng ở bất kỳ môi trường dùng chung nào.
- Không dùng `ddl-auto=update` thay cho migration.
- Không thêm `IF NOT EXISTS` để che giấu schema drift trong versioned migration.
- Migration cần chạy được trên database trống và chạy lại ứng dụng không phát sinh thay đổi ngoài ý muốn.
- Backup và kiểm thử restore là quy trình riêng, bắt buộc trước khi migrate production.

### Database đã tồn tại trước Flyway

Không bật `baseline-on-migrate` một cách máy móc. Cần đối chiếu schema thực tế, chọn baseline version đúng với trạng thái database, backup trước và diễn tập trên bản sao staging. Baseline sai có thể khiến Flyway bỏ qua migration cần thiết hoặc đánh dấu lịch sử không đúng.

### Seed dữ liệu

`V2__reference_data.sql` chứa dữ liệu tham chiếu và mapping role/permission. Ngoài ra dự án có `DataSeeder`, nhưng chỉ hoạt động khi đồng thời:

- Profile `seed-once` được bật.
- Property `app.seeder.enabled=true` được cung cấp.

Seeder này có thao tác dữ liệu và chỉ dành cho khởi tạo có chủ đích. Không bật `seed-once` trong production thường xuyên.

## Chạy backend

### Development

```powershell
cd demo
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Profile `dev` bật SQL logging, Swagger/OpenAPI và hiển thị chi tiết health check.

### Build JAR

```powershell
cd demo
.\mvnw.cmd -B clean package
```

Artifact được tạo trong `demo/target/`.

### Chạy JAR với profile production

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
java -jar .\target\demo-0.0.1-SNAPSHOT.jar
```

Profile `prod` tắt Swagger, tắt SQL logging và chỉ công khai health/info với mức chi tiết giới hạn.

## Chạy frontend

### Development server

```powershell
cd frontend
npm ci
npm start
```

### Production build

```powershell
cd frontend
npm ci
npm run build -- --configuration production
```

Kết quả build nằm trong `frontend/dist/` theo cấu hình Angular CLI.

> Lưu ý: `frontend/src/environments/environment.ts` hiện vẫn đặt `apiUrl` là `http://localhost:8080`. Trước khi production phải đổi sang URL HTTPS thật của API hoặc áp dụng cơ chế cấu hình runtime/build-time phù hợp. Không phát hành bundle production còn trỏ về localhost.

Frontend có dependency SSR trong `package.json`, nhưng cấu hình build hiện dùng browser application và không khai báo server entry. Quy trình deploy hiện tại nên coi frontend là static SPA, trừ khi SSR được thiết kế và kiểm thử lại có chủ đích.

## API và phân quyền

Base path của REST API:

```text
/api/v1
```

Nhóm endpoint chính:

| Nhóm | Base path | Truy cập điển hình |
|---|---|---|
| Authentication | `/api/v1/auth` | Public cho login/register/refresh/social |
| Category | `/api/v1/categories` | Public read; write cần quyền |
| Brand | `/api/v1/brands` | Public read; write cần quyền |
| Product/variant | `/api/v1/products`, `/api/v1/variants` | Public read; write cần quyền |
| Cart | `/api/v1/cart` | Theo session/user và rule endpoint |
| Orders | `/api/v1/orders` | Tạo đơn/track guest public; đơn cá nhân cần đăng nhập |
| Reviews | `/api/v1/products/{id}/reviews` | Public read; write có kiểm soát |
| Inventory | `/api/v1/inventory` | Stock summary public; nghiệp vụ kho cần quyền |
| Returns | `/api/v1/returns` | Khách hàng đã xác thực |
| News/banner/settings | `/api/v1/news`, `/api/v1/banners`, `/api/v1/settings` | Public read có giới hạn; admin write |
| Admin | `/api/v1/admin/**` | Role/permission backend |
| Upload | `/api/v1/upload` | Người dùng đã xác thực |
| Chat | `/api/v1/chat`, `/api/v1/admin/chat` | Guest/customer và admin tách biệt |
| WebSocket | `/ws-chat` | SockJS/STOMP handshake |

### Luồng JWT

1. Client login và nhận access token/refresh token.
2. Access token được gửi bằng header `Authorization: Bearer <token>`.
3. Khi access token hết hạn, frontend dùng refresh token để lấy token mới.
4. Các request 401 đồng thời chia sẻ một refresh stream để tránh refresh race condition.
5. Logout/revoke làm token không còn hợp lệ theo chính sách backend.

### Bảo vệ đơn hàng

- `POST /api/v1/orders`: cho phép guest checkout.
- `GET /api/v1/orders/track?code=...&phone=...`: tra cứu đơn guest bằng hai yếu tố thông tin.
- `GET /api/v1/orders/{orderCode}`: yêu cầu đăng nhập và chỉ trả đơn thuộc về người dùng hiện tại.
- Không mở lại endpoint chi tiết đơn chỉ dựa trên mã đơn; việc đoán mã có thể làm lộ dữ liệu cá nhân.

### Phân quyền

Backend áp dụng `@PreAuthorize`/Spring Security theo role và permission. Guard phía Angular chỉ hỗ trợ trải nghiệm người dùng, không thay thế kiểm tra quyền phía server.

Các role nền tảng gồm admin, staff và customer; mapping quyền được seed bằng Flyway. Khi thêm endpoint quản trị mới, phải bổ sung đồng thời:

- Permission backend.
- Mapping role/permission trong migration mới.
- `@PreAuthorize` phù hợp.
- Guard/menu visibility ở frontend.
- Test cho trường hợp được phép và bị từ chối.

## Redis và cache

Redis được sử dụng cho Spring Cache và một số cơ chế bảo vệ như rate limiting đăng nhập.

TTL được khai báo riêng theo cache, ví dụ:

| Cache | TTL |
|---|---:|
| Product list | 15 phút |
| Product detail | 30 phút |
| Category/brand/attribute | 2 giờ |
| Banner/chat bot rule | 1 giờ |
| News/review detail | 15–30 phút |
| Statistics | 15 phút |
| System settings | 24 giờ |

Các thao tác create/update/delete phải evict cache liên quan. Không cache cart, order hoặc số lượng tồn kho như nguồn dữ liệu quyết định vì đây là dữ liệu thay đổi nhanh và yêu cầu tính chính xác.

## Upload và tài nguyên tĩnh

- Upload local được ghi vào thư mục `uploads/`.
- Tài nguyên được phục vụ công khai qua `/uploads/**`.
- Giới hạn mặc định: 5 MB mỗi file và 10 MB mỗi request multipart.
- Upload chat có rate limit riêng.
- Thư mục upload được Git ignore và không được đóng gói như source code.

Production cần dùng persistent volume hoặc S3-compatible object storage. Không lưu dữ liệu người dùng chỉ trong filesystem tạm của container vì dữ liệu sẽ mất khi recreate instance. Reverse proxy cũng phải giới hạn body size tương thích với backend.

## Kiểm thử

### Backend

Chạy toàn bộ test:

```powershell
cd demo
.\mvnw.cmd -B test
```

Chạy quy trình tương đương CI:

```powershell
cd demo
.\mvnw.cmd -B clean verify
```

Kiểm tra Flyway riêng:

```powershell
cd demo
.\mvnw.cmd -B "-Dtest=FlywayMigrationIsolatedTest" test
```

`FlywayMigrationIsolatedTest` tạo database có tên UUID, chạy migration/validate rồi chỉ xóa database vừa tạo.

> Cảnh báo: cấu hình test mặc định hiện dùng `DB_NAME=computer_store_db`. Khi chạy toàn bộ integration test trên máy cá nhân, hãy đặt `DB_NAME` thành database test riêng, ví dụ `computer_store_test`, và tuyệt đối không dùng credentials production. Hướng cải tiến khuyến nghị là Testcontainers hoặc fail-fast guard bắt buộc hậu tố `_test`.

### Frontend

```powershell
cd frontend
npm ci
npm run test -- --watch=false
npm run build -- --configuration production
```

Test runner là Vitest/jsdom. Không truyền cờ `--browsers=ChromeHeadless` của Karma.

Lệnh lint trong workflow dùng `npm run lint --if-present`; hiện `package.json` chưa khai báo script `lint`. Nếu muốn lint trở thành quality gate thực sự, cần cấu hình ESLint và thêm script tương ứng.

## CI/CD

Hai workflow nằm trong `.github/workflows/`:

- `backend-ci.yml`: chạy khi thay đổi `demo/**`, `database/**` hoặc workflow backend; dựng MySQL 8 + Redis 7 và chạy `./mvnw -B clean verify` trên JDK 21.
- `frontend-ci.yml`: chạy khi thay đổi `frontend/**` hoặc workflow frontend; dùng Node 22, `npm ci`, Vitest và production build.

Workflow áp dụng cho branch `main` và `develop`. Các secret liên quan được mô tả tại [`docs/github-actions-secrets.md`](docs/github-actions-secrets.md).

Không được bỏ qua test lỗi, disable test để làm xanh pipeline hoặc merge quanh branch protection.

## Triển khai production

Repository hiện chưa cung cấp Docker Compose/Dockerfile hoặc manifest hạ tầng production hoàn chỉnh. Mô hình deploy tối thiểu đề xuất:

```text
Internet
   -> HTTPS reverse proxy/load balancer
      -> Angular static files
      -> /api, /uploads, /ws-chat -> Spring Boot
                                      -> MySQL 8
                                      -> Redis 7
                                      -> SMTP/OAuth providers
```

### Backend

- Chạy với `SPRING_PROFILES_ACTIVE=prod`.
- Dùng non-root OS/container user.
- Chỉ cấp DB user quyền cần thiết; quyền migration có thể tách khỏi runtime user nếu quy trình deploy hỗ trợ.
- Chỉ tin `X-Forwarded-*` từ reverse proxy thuộc dải trusted/internal proxies.
- Chỉ expose cổng backend trong private network nếu proxy đứng phía trước.
- Dùng health endpoint cho readiness/liveness probe.
- Bảo đảm instance có persistent storage cho uploads hoặc chuyển sang object storage.

### Frontend

- Đổi `environment.ts` sang API HTTPS production trước khi build.
- Phục vụ SPA bằng Nginx/CDN/static hosting và cấu hình fallback route về `index.html`.
- Cache asset có hash dài hạn; không cache `index.html` quá lâu.
- Không expose source map production nếu không có nhu cầu và kiểm soát truy cập.

### Reverse proxy

- Bắt buộc HTTPS và redirect HTTP sang HTTPS.
- Forward đúng `Host`, `X-Forwarded-For`, `X-Forwarded-Proto`.
- Hỗ trợ WebSocket upgrade cho `/ws-chat`.
- Route `/api/**` và `/uploads/**` về backend nếu dùng cùng domain.
- Giới hạn request body, timeout và rate limit phù hợp.
- Không chấp nhận forwarded headers trực tiếp từ Internet ngoài proxy tin cậy.

### Database và Redis

- Backup MySQL trước mỗi migration production và kiểm tra restore định kỳ.
- Chạy migration trên staging từ bản sao dữ liệu gần production trước.
- MySQL/Redis không được public trực tiếp ra Internet.
- Bật authentication/TLS theo hạ tầng và lưu credentials trong secret manager.
- Thiết lập monitoring cho connection pool, slow query, disk usage và cache availability.

## Production checklist

### Bắt buộc trước release

- [ ] Working tree sạch; release gắn với commit/tag cụ thể.
- [ ] Review toàn bộ migration mới; không sửa migration đã áp dụng.
- [ ] Backup database và thử restore thành công.
- [ ] Migration chạy thành công trên staging có dữ liệu gần production.
- [ ] Backend `mvnw -B clean verify` thành công.
- [ ] Frontend test và production build thành công.
- [ ] GitHub Actions xanh trên commit phát hành.
- [ ] Test DB được cô lập hoàn toàn khỏi dev/production DB.
- [ ] Frontend production không còn trỏ tới `localhost:8080`.
- [ ] `SPRING_PROFILES_ACTIVE=prod` được thiết lập.
- [ ] JWT, database, Redis, mail và OAuth secrets nằm trong secret manager.
- [ ] CORS chỉ chứa domain production thật; không dùng `*`.
- [ ] HTTPS, trusted proxy headers và WebSocket upgrade đã kiểm tra.
- [ ] Upload dùng persistent storage và đã có chiến lược backup.
- [ ] Không có tài khoản/mật khẩu mặc định hoặc secret trong log.
- [ ] Smoke test login/refresh/logout và role admin/staff/customer.
- [ ] Smoke test guest checkout và `/orders/track` với cả trường hợp đúng/sai.
- [ ] Xác nhận người dùng không thể xem đơn của tài khoản khác.
- [ ] Smoke test tồn kho, checkout, hủy đơn và cập nhật trạng thái.
- [ ] Health check, log aggregation, alerting và rollback procedure hoạt động.

### Khuyến nghị sau release

- [ ] Theo dõi tỷ lệ 4xx/5xx, latency, JVM memory, DB pool và Redis errors.
- [ ] Cảnh báo dung lượng database/upload disk.
- [ ] Rate limit ở reverse proxy/WAF cho auth, track order và upload.
- [ ] Chạy dependency/security scanning định kỳ.
- [ ] Diễn tập rollback ứng dụng và restore database.
- [ ] Thiết lập retention/redaction cho audit log và dữ liệu cá nhân.

## Xử lý sự cố

### Backend không khởi động vì `JWT_SECRET`

Khai báo một secret đủ dài trước khi chạy:

```powershell
$env:JWT_SECRET = "replace-with-a-long-random-secret"
```

Không dùng secret mẫu trong production.

### Flyway báo checksum mismatch

Nguyên nhân phổ biến là migration đã áp dụng bị chỉnh sửa. Không xóa lịch sử hoặc chạy `repair` ngay để che lỗi. Hãy:

1. Dừng deploy.
2. So sánh file migration với commit đã triển khai.
3. Khôi phục migration cũ.
4. Viết migration version mới cho thay đổi tiếp theo.

Chỉ dùng `repair` sau khi hiểu rõ nguyên nhân và có backup.

### Hibernate báo schema validation failed

Kiểm tra:

- Flyway đã chạy đủ migration chưa.
- Ứng dụng có đang trỏ đúng database không.
- Entity và schema có bị thay đổi ngoài migration không.
- User database có đủ quyền đọc metadata/chạy migration không.

### Redis không kết nối được

```powershell
redis-cli -h localhost -p 6379 ping
```

Kiểm tra `REDIS_HOST`, `REDIS_PORT`, firewall và trạng thái Redis. Các luồng phụ thuộc rate limiting/cache có thể bị ảnh hưởng dù MySQL vẫn hoạt động.

### Frontend gọi nhầm API

Kiểm tra `frontend/src/environments/environment.ts` và `environment.development.ts`, sau đó build lại. Nếu frontend/API khác origin, xác nhận origin frontend đã có trong `CORS_ALLOWED_ORIGINS`.

### API trả 401/403

- `401 Unauthorized`: token thiếu, hết hạn, sai chữ ký hoặc bị revoke.
- `403 Forbidden`: đã xác thực nhưng thiếu role/permission.
- Kiểm tra header `Authorization`, refresh flow và mapping permission trong database.

### WebSocket chat không kết nối

Kiểm tra endpoint `/ws-chat`, CORS, cấu hình reverse proxy `Upgrade`/`Connection` và URL backend trong frontend.

## Quy ước phát triển

Đọc [`AGENTS.md`](AGENTS.md) trước khi thay đổi code. Một số nguyên tắc quan trọng:

- Backend dùng constructor injection; controller không gọi repository trực tiếp.
- Không trả JPA entity trực tiếp từ API; dùng request/response DTO.
- Business validation và transaction đặt ở service layer.
- Mọi thay đổi schema đi qua Flyway migration mới.
- API admin phải có kiểm tra quyền backend.
- Frontend dùng standalone components, lazy routes và domain service.
- Storefront và admin không import UI chéo; thành phần chung đặt trong `shared/`.
- Không cache cart, order hoặc tồn kho real-time.
- Không commit secret, upload runtime, build artifact hoặc dependency directory.
- Không bỏ qua test/CI đang lỗi.

### Git workflow

- `main`: production.
- `develop`: staging/integration.
- `feature/<name>`: tính năng.
- `fix/<name>`: sửa lỗi.

Commit theo Conventional Commits:

```text
feat(product): add variant filtering
fix(order): restrict order detail to its owner
test(flyway): isolate migration database
docs(readme): document production setup
```

Pull request cần mô tả mục đích, bảng DB bị ảnh hưởng, cách kiểm thử và việc cache eviction đã được xác minh hay chưa.

## Tài liệu liên quan

- [`AGENTS.md`](AGENTS.md): kiến trúc, coding conventions và giới hạn phạm vi.
- [`docs/github-actions-secrets.md`](docs/github-actions-secrets.md): cấu hình GitHub Actions secrets.
- [`docs/zalo-oauth-walkthrough.md`](docs/zalo-oauth-walkthrough.md): luồng Zalo OAuth/PKCE.
- [`database/database_ban_may_tinh.sql`](database/database_ban_may_tinh.sql): schema SQL tham khảo ban đầu; migration Flyway mới là nguồn lịch sử triển khai runtime.

## License

Repository hiện chưa khai báo license. Mặc định không được coi source code là phần mềm nguồn mở cho đến khi chủ dự án bổ sung file `LICENSE` và điều khoản sử dụng rõ ràng.
