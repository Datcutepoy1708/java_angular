# AGENTS.md — Computer & PC Components E-commerce Platform

This file guides AI coding agents (Claude Code, Cursor, etc.) working on this repository. Place this file at the **repo root** if using a monorepo (backend + frontend together). If backend and frontend live in separate repos, keep a separate `AGENTS.md` in each, containing only the relevant sections.

## 1. Project overview

E-commerce platform for selling computers & PC components:
- **Storefront**: browse products, filter by technical specs, cart, checkout, reviews, news/blog.
- **Admin panel**: manage products, inventory, orders, discount codes, news, banners, dashboard/statistics.

## 2. Tech stack

| Layer            | Technology                                         |
|-------------------|----------------------------------------------------|
| Backend           | Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Frontend          | Angular 18+, TypeScript, RxJS, Angular Material or PrimeNG |
| Database          | MySQL 8.x                                           |
| Cache             | Redis 7.x (via Spring Cache abstraction + Lettuce client) |
| Auth              | JWT (access + refresh token), BCrypt password hashing |
| Build tool BE     | Maven                                               |
| Build tool FE     | npm / Angular CLI                                   |
| API docs          | OpenAPI 3 (springdoc-openapi)                       |
| DB migration      | Flyway                                              |
| File upload       | Local `/uploads` (dev) or S3-compatible storage (prod) |

## 3. Current priority: Basic CRUD first

**Do not build the full feature set yet.** The current phase is to get basic CRUD working end-to-end (Backend API → Frontend consuming it) for a few core entities, with Redis caching wired in from the start so the pattern is established early. Priority order:

1. **Category** (simple CRUD, cache TTL long — small dataset, changes rarely)
2. **Brand** (simple CRUD)
3. **Product** (CRUD + list with pagination/filter — this is where caching matters most, high read volume)
4. **Product Variant** (CRUD, nested under Product)

Do not start Order, Cart, Discount, Inventory, News, or Banner modules until the above CRUD + caching pattern is solid and reviewed. This keeps the codebase pattern consistent before scaling to more complex modules (which involve multi-table transactions).

## 4. Project structure

This repo is named `java_angular`, with two actual top-level module folders: **`demo`** (Spring Boot backend) and **`frontend`** (Angular). Use these exact folder names — do not rename them to `backend/` when generating code or paths.

```
java_angular/                  # repo root
├── .github/                   # CI/CD workflows (GitHub Actions)
├── demo/                      # Spring Boot backend (Java)
│   ├── src/main/java/com/store/
│   │   ├── config/          # SecurityConfig, CorsConfig, RedisConfig, SwaggerConfig, JwtConfig
│   │   ├── controller/      # REST controllers, one per domain
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── entity/          # JPA entities, mapped to database_ban_may_tinh.sql
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── service/
│   │   │   └── impl/
│   │   ├── mapper/          # MapStruct mapper Entity <-> DTO
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   ├── security/        # JwtTokenProvider, UserDetailsServiceImpl, filters
│   │   └── util/
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway: V1__init.sql, V2__seed_data.sql...
│   │   └── application.yml
│   └── pom.xml
├── frontend/                  # Angular frontend
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── guards/
│   │   │   ├── interceptors/
│   │   │   └── services/
│   │   ├── shared/
│   │   ├── features/
│   │   │   ├── shop/
│   │   │   ├── admin/
│   │   │   └── auth/
│   │   └── layout/
│   ├── src/environments/
│   └── angular.json
├── database/
│   └── database_ban_may_tinh.sql
└── AGENTS.md
```

> Note: the backend package name (`com.store`) is a placeholder — check `demo/pom.xml` (`groupId`/`artifactId`) and the actual generated package under `demo/src/main/java/` for the real base package, and use that instead when generating new classes.

## 5. Redis caching conventions (backend)

### 5.1 Setup
- Use `spring-boot-starter-data-redis` + `spring-boot-starter-cache`.
- Enable caching with `@EnableCaching` in a `CacheConfig` class.
- Configure `RedisCacheManager` with **explicit TTL per cache name** (never rely on one blind global default):

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> configs = new HashMap<>();
    configs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(15)));
    configs.put("productDetail", defaultConfig.entryTtl(Duration.ofMinutes(30)));
    configs.put("categories", defaultConfig.entryTtl(Duration.ofHours(2)));
    configs.put("brands", defaultConfig.entryTtl(Duration.ofHours(2)));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(configs)
        .build();
}
```

### 5.2 What to cache (and what NOT to)
- **Cache**: read-heavy, rarely-changing data — product list, product detail, category tree, brand list.
- **Do NOT cache**: cart, order, inventory quantity (must always be real-time/accurate — stale stock data causes overselling), user session data (use JWT instead of a session cache).
- Never cache anything containing user-specific sensitive data (e.g. don't cache a response that includes `password_hash` or a personal address).

### 5.3 Cache annotation pattern
```java
@Cacheable(cacheNames = "productDetail", key = "#productId")
public ProductResponse getProductById(Long productId) { ... }

@CacheEvict(cacheNames = {"products", "productDetail"}, allEntries = true)
public ProductResponse updateProduct(Long productId, ProductRequest request) { ... }

@CacheEvict(cacheNames = "productDetail", key = "#productId")
public void deleteProduct(Long productId) { ... }
```
- On **create/update/delete**, always evict the relevant cache entries in the same service method — never let stale cache outlive a write operation.
- For paginated product-list caches, prefer `allEntries = true` eviction on write (list caches with different filter/page params are hard to target individually), rather than leaving stale pages behind.
- Cache key naming: include enough parameters to be unique (e.g. `products::category_5_page_0_size_20`) — use a custom `KeyGenerator` if the default key generation isn't specific enough.

### 5.4 Redis for other use cases (later phases, not now)
- Rate limiting login attempts.
- Storing refresh tokens (optional alternative to the `auth_tokens` MySQL table) for faster lookup/revocation.
- Distributed locking (`Redisson`) for inventory deduction during high-concurrency checkout — relevant once the Order module is built.

## 6. Backend conventions (Spring Boot)

### 6.1 Naming
- Entity: PascalCase singular, matching the table name plural → table `products` → entity `Product`.
- Repository: `ProductRepository extends JpaRepository<Product, Long>`.
- Service: `ProductService` (interface) / `ProductServiceImpl`.
- DTO: `ProductRequest`, `ProductResponse` — never expose entities directly in controllers.
- REST endpoints: plural, kebab-case for multi-word paths → `/api/v1/products`, `/api/v1/discount-codes`.

### 6.2 Layered architecture (mandatory)
```
Controller → Service (interface) → ServiceImpl → Repository → Entity
```
- Controllers contain no business logic — only receive requests, validate, call service, return response.
- Controllers never call Repository directly.
- Use `@Transactional` at the Service layer for multi-table writes.

### 6.3 Standard API response wrapper
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}
```
Use `@RestControllerAdvice` (`GlobalExceptionHandler`) to catch exceptions and return a consistent `ApiResponse` — never leak stack traces to the client.

### 6.4 Authorization
- Use `@PreAuthorize("hasRole('ADMIN')")` or `@PreAuthorize("hasAuthority('PRODUCT_CREATE')")`, matching the `roles`/`permissions` tables.
- JWT payload contains: `user_id`, `email`, `roles[]`.
- Short-lived access token (~15-30 min), long-lived refresh token stored in `auth_tokens`.

### 6.5 Validation
- Use `jakarta.validation` annotations (`@NotBlank`, `@Positive`, `@Email`...) on request DTOs.
- Business-rule validation (e.g. sufficient stock, discount still valid) belongs in the Service layer, throwing custom exceptions.

### 6.6 Dependency Injection (mandatory rule)
- **Always use constructor injection**, never field injection. Use Lombok `@RequiredArgsConstructor` on the class plus `private final` fields for every injected dependency:
  ```java
  @Service
  @RequiredArgsConstructor
  public class ProductServiceImpl implements ProductService {
      private final ProductRepository productRepository;
      private final BrandRepository brandRepository;
  }
  ```
- **Never write:**
  ```java
  @Autowired
  private ProductRepository productRepository; // forbidden — field injection
  ```
  Field injection is banned in this codebase: it hides required dependencies, allows partially-constructed objects, and can't be unit-tested without a Spring context.
- Depend on **interfaces, not concrete classes**, wherever a Service is injected (e.g. `ProductService`, not `ProductServiceImpl`) — this keeps layers swappable and mockable in tests.
- Every Service/Controller/Component-equivalent class must be trivially testable via `new XyzServiceImpl(mockDep1, mockDep2)` without booting Spring — if it isn't, DI usage is wrong somewhere.
- Circular dependencies between services are a design smell — refactor (extract shared logic into a third service) instead of using `@Lazy` to paper over it.

## 7. Frontend conventions (Angular)

### 7.0 Project baseline decisions (settled — do not re-litigate per task)
- **No SSR.** Angular CLI defaults to scaffolding SSR (`main.server.ts`, `server.ts`, `app.config.server.ts`, `app.routes.server.ts`). This project does not use it during development — delete those files and the SSR-related entries in `angular.json` (`"ssr"` block) and `package.json` (`"serve:ssr:*"` scripts) right after `ng new`. Revisit adding SSR back via `ng add @angular/ssr` only when SEO on the public storefront becomes a real concern near launch, not before.
- **Component file naming keeps the `.component` suffix**, overriding the newer Angular CLI default that drops it (`app.ts`/`app.html` instead of `app.component.ts`/`app.component.html`). Always generate/name component files as `xyz.component.ts`, `xyz.component.html`, `xyz.component.scss` — this matches every component example already established in this codebase and keeps component files visually distinct from services/guards/models at a glance. Rename the CLI-generated root `app.ts`/`app.html`/`app.css` to `app.component.ts`/`app.component.html`/`app.component.scss` (updating the selector/bootstrap reference accordingly) to stay consistent.
- Keep `app.spec.ts` (unit test scaffold), `.editorconfig`, `angular.json`, `tsconfig*.json` — these are normal, not clutter.

### 7.1 Architecture
- Use **standalone components** (Angular 18+), lazy-loaded per feature route (`shop`, `admin`, `auth`).
- Use Angular Signals for simple state; consider NgRx only if state complexity grows (e.g. cart syncing across tabs).

### 7.2 API services
- One service per domain: `product.service.ts`, `category.service.ts`, `auth.service.ts`.
- TypeScript models in `core/models/`, named to match backend response DTOs.
- `HttpInterceptor`s:
  - `jwt.interceptor.ts`: attach `Authorization: Bearer <token>`.
  - `error.interceptor.ts`: handle 401 (refresh token or redirect to login), 403 (insufficient-permission notice).

### 7.3 Guards
- `auth.guard.ts`: block routes requiring login.
- `role.guard.ts`: block admin routes based on roles decoded from the JWT — but this is a UX convenience only; **real enforcement must always happen on the backend**.

### 7.4 Dependency Injection (mandatory rule)
- Services are always registered with `@Injectable({ providedIn: 'root' })` (tree-shakable singleton) unless there's a specific reason for a component/module-scoped provider — state that reason in a comment if you deviate.
- In **standalone components** (the default in this project), use the functional `inject()` API for dependencies:
  ```typescript
  export class BrandManageComponent {
    private readonly brandService = inject(BrandService);
    private readonly fb = inject(FormBuilder);
  }
  ```
- In services, use classic constructor injection:
  ```typescript
  constructor(private readonly http: HttpClient) {}
  ```
- Never instantiate a service manually with `new SomeService()` — always let Angular's injector provide it, so interceptors, testing overrides, and singleton behavior work correctly.
- Never inject `HttpClient` directly into a component — always go through a domain service (`BrandService`, `ProductService`...) so API logic stays testable and reusable, per section 7.2.

### 7.5 Static assets (images, video, design references)
- **`frontend/public/`**: production assets actually served to end users — logos, product images, favicon, hero video/poster. Anything here ends up in the deployed bundle and is publicly reachable by URL.
- **`design/`** (repo root, outside `frontend/`): Stitch mockup exports (`.md` design tokens, screenshot `.png` references). These are for agent/developer reference only — never referenced at runtime, never imported into Angular code.
- **Responsive media rule**: heavy assets (video) are desktop-only; mobile always gets a lightweight static image fallback, never the video, to save mobile bandwidth and avoid autoplay restrictions on mobile browsers. Pattern:
  ```html
  <video class="hero-video hidden md:block" autoplay muted loop playsinline poster="/videos/hero-poster.jpg">
    <source src="/videos/homepage-hero.mp4" type="video/mp4">
  </video>
  <img class="hero-image block md:hidden" src="/videos/hero-poster.jpg" alt="...">
  ```
  Breakpoint used for this split must match `$breakpoint-tablet` already defined in `frontend/src/styles/_typography.scss` (~768px) — don't invent a new breakpoint value elsewhere.
- Any video asset must be compressed (H.264, target well under 5MB for a ~10s clip) and audio-stripped before being placed in `public/` — autoplay banners are always muted, so shipping an audio track is dead weight.
- Always pair a hero video with a static poster image (extracted frame) for the `poster` attribute and for the mobile fallback — never leave `<video>` without a `poster`.

### 7.6 Admin vs Client separation

The storefront (public-facing shop) and the admin panel are two distinct experiences sharing one Angular app. Keep them cleanly separated at every layer — routing, layout shell, and feature folders — so neither leaks into the other and each can be worked on independently.

**Folder structure** (expand on the `features/` skeleton already defined in section 4):
```
src/app/
├── core/                    # cross-cutting: guards, interceptors, models, services
├── layout/
│   ├── public-shell/        # header + footer wrapper for storefront pages
│   │   └── public-shell.component.ts/html/scss
│   └── admin-shell/         # sidebar + top header wrapper for admin pages
│       └── admin-shell.component.ts/html/scss
├── features/
│   ├── auth/                # login, register, admin-login — shared entry point, no shell
│   ├── shop/                 # storefront only: home, category, product-detail, cart, checkout
│   │   ├── home/
│   │   ├── product-listing/
│   │   ├── product-detail/
│   │   ├── cart/
│   │   └── checkout/
│   └── admin/                 # admin only: dashboard, product-manage, order-manage, etc.
│       ├── dashboard/
│       ├── brand-manage/
│       ├── category-manage/
│       ├── product-manage/
│       ├── order-manage/
│       └── statistics/
```

**Routing rule — one shell per route tree, lazy-loaded, path-prefixed:**
```typescript
// app.routes.ts
export const routes: Routes = [
  { path: '', component: PublicShellComponent, children: [
      { path: '', loadComponent: () => import('./features/shop/home/home.component')... },
      { path: 'products/:categorySlug', loadComponent: () => ... },
      { path: 'cart', loadComponent: () => ... },
    ]
  },
  { path: 'admin', component: AdminShellComponent, canActivate: [authGuard, roleGuard], children: [
      { path: 'dashboard', loadComponent: () => ... },
      { path: 'products', loadComponent: () => ... },
    ]
  },
  { path: 'auth/login', loadComponent: () => import('./features/auth/login/login.component')... },
  { path: 'auth/register', loadComponent: () => import('./features/auth/register/register.component')... },
  { path: 'auth/admin-login', loadComponent: () => import('./features/auth/admin-login/admin-login.component')... },
];
```
- Every route under `/admin/**` carries `authGuard` + `roleGuard` (checking for `ROLE_ADMIN`/`ROLE_STAFF`) at the parent route level — do not re-add the guard on every child route individually; the parent's `canActivate` already protects all children.
- `features/shop/**` never imports anything from `features/admin/**` and vice versa. If a component is genuinely needed by both (e.g. a product card), it belongs in `shared/`, not duplicated or cross-imported.
- Services in `core/services/` (e.g. `product.service.ts`) are shared by both — the split is about UI/routes/layout, not about duplicating API-calling logic. Admin and Client both call the same `ProductService`, just from different components with different permissions enforced server-side.
- Two shells (`public-shell`, `admin-shell`) must not share a layout component even if visually similar — keep them as separate components since their structure (header+footer vs sidebar+topbar) diverges enough that forcing a shared shell adds more conditional complexity than it saves.

## 8. Database conventions

- `database/database_ban_may_tinh.sql` is the source of truth for schema.
- All schema changes go through a new Flyway migration file — never edit an already-applied `V1__init.sql` in place.
- Prefer soft-delete (`status` column) over hard `DELETE` for orders/users.

## 9. Security (mandatory)

- Passwords: BCrypt, never log or return `password_hash`.
- Never expose the full `User` entity via API — always map through DTOs.
- Validate input on both FE (UX) and BE (real security) — never trust FE-only validation.
- Use parameterized queries via JPA — no manually concatenated SQL strings.
- File uploads: restrict file type/size, randomize filenames (UUID) to prevent path traversal.
- CORS: restrict to the frontend's actual origin in production, never `*`.

## 10. Testing

- Backend: JUnit 5 + Mockito for the service layer; use `@SpringBootTest` with the CI-provided MySQL/Redis service containers for current integration tests, including cache-eviction-on-write behavior. Testcontainers is a future improvement and must not be claimed as active until its dependencies and container-backed tests actually exist.
- Frontend: Vitest for unit tests (Angular's current default, replacing Karma); Cypress/Playwright for e2e on core flows once they exist.
- Whenever Redis caching is added to a CRUD endpoint, write a test verifying that an update/delete actually evicts the cache (stale reads are a common bug class here).

## 11. Git & commit conventions

- Branches: `main` (production), `develop` (staging), `feature/<name>`, `fix/<name>`.
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`.
  - Example: `feat(product): add Redis caching for product detail endpoint`
- PRs must describe: purpose, affected DB tables (if any), how it was tested, and whether cache eviction was verified.
- **No decorative icon/emoji badges or trust badges.** Do not add small colorful icon+text chip elements like "🚀 50K+ Products" or "🚚 Free Shipping" — these decorative trust-badge patterns (common in AI-generated hero sections) are visual clutter and must be removed or replaced with plain text if the content is worth keeping (e.g. plain text "50,000+ products in stock" with no icon/emoji in front of it).
  - If a Stitch mockup includes these badges, treat it as reference for layout only — strip the icon/emoji and either drop the badge entirely or keep it as plain text, when implementing the real Angular component.
  - This also applies to commit messages, PR descriptions, code comments, and log messages — no decorative emoji there either (e.g. `feat(product): ✨ add search 🔍` → `feat(product): add search`).
  - **Functional UI icons are still allowed where they materially aid usability** — e.g. a cart icon with item-count badge in the header (no room for text label there), a password show/hide toggle icon, a search magnifying-glass inside a search input, a notification bell in the admin topbar, a dark-mode toggle, an "opens in new tab" external-link glyph. Keep these minimal, single-color/monochrome (matching `--on-surface-variant` from the design tokens), and consistent with the design system — not colorful, not multi-color, not emoji-style. If in doubt whether an icon is "functional" or "decorative," ask before adding it.

## 12. Continuous Integration (GitHub Actions)

CI configuration lives in `.github/workflows/`. Every PR into `main` or `develop` must pass CI before merge — do not bypass or skip failing checks by disabling a workflow step.

### 12.1 Required workflows
- **`backend-ci.yml`** — triggers on changes under `demo/**`:
  1. Checkout, set up JDK 21.
  2. Start the required MySQL/Redis service containers, then run `mvn -B verify` (compiles and runs unit/integration tests).
  3. Fail the build on any test failure — do not mark tests `@Disabled` to force a green build.
- **`frontend-ci.yml`** — triggers on changes under `frontend/**`:
  1. Checkout, set up Node LTS, `npm ci` (never `npm install` in CI — lockfile must be respected exactly).
  2. `npm run lint`.
  3. `npm run test -- --watch=false` (Vitest; do not pass Karma-only flags such as `--browsers=ChromeHeadless`).
  4. `npm run build -- --configuration production` to catch build-time errors early.
- Use `paths:` filters so backend changes don't trigger the frontend workflow and vice versa — keeps CI fast and cheap.

### 12.2 Rules for the agent
- Any new feature that adds a Maven dependency, npm package, or new test command must also update the corresponding workflow file if the existing steps no longer cover it. If Testcontainers is introduced, update the test infrastructure deliberately and do not also start duplicate CI service containers for the same dependency.
- Never commit code that fails `mvn verify` or `npm run lint` locally — CI is a safety net, not a substitute for running checks before pushing.
- Secrets used in CI (DB password for integration tests, etc.) must be referenced via GitHub Actions `secrets.*` context — never hardcoded in the workflow YAML.
- When adding a new Redis-dependent test (per section 10), ensure exactly one supported Redis test environment exists: either the workflow Redis service (`redis:7-alpine`) or a Testcontainers-managed Redis instance.
- Branch protection on `main`/`develop` requires both `backend-ci` and `frontend-ci` (when relevant paths changed) to pass, plus at least one review approval — do not suggest merging around this.

## 13. Production hardening rules

### 13.1 Order data access

- `GET /api/v1/orders/{orderCode}` is authenticated-only and may return an order only when `order.user.userId` matches the authenticated user ID.
- Guest orders must never be returned from the authenticated order-detail endpoint. Guests must use `GET /api/v1/orders/track?code=...&phone=...`, and both values must match.
- A failed guest lookup must return a generic error that does not reveal whether the order code exists.
- Any change to order access must include HTTP-level regression tests for anonymous access, wrong ownership, member access to a guest order, owner access, and successful/failed guest tracking.

### 13.2 JWT, credentials, and bootstrap accounts

- JWT signing secrets must have no source-code fallback or default value. Production keys must come from environment variables or a secret manager and provide at least 256 bits of random entropy.
- Required secrets must be validated at startup without logging their contents; missing, malformed, or weak production secrets must fail fast.
- Never log passwords, JWTs, refresh/reset tokens, OTPs, cookies, authorization headers, or other credential material.
- Admin bootstrap is disabled by default and may run only when `ADMIN_BOOTSTRAP_ENABLED=true` and all required values such as `ADMIN_INITIAL_EMAIL` and `ADMIN_INITIAL_PASSWORD` are present.
- If bootstrap is enabled but incomplete, startup must fail. Never generate or print an initial admin password in application logs.

### 13.3 Trusted proxy and client IP

- Controllers must not read `Forwarded`, `X-Forwarded-For`, or `X-Real-IP` directly. Resolve client IP through one shared `ClientIpResolver` using `HttpServletRequest#getRemoteAddr()`.
- For Tomcat behind a trusted reverse proxy, use `server.forward-headers-strategy: native` with an explicit `server.tomcat.remoteip.internal-proxies` allowlist. Do not combine Tomcat native proxy rules with the framework forwarding strategy.
- The backend must not be directly reachable around the trusted proxy. The proxy must remove client-supplied forwarding headers and set its own trusted values.
- Do not enable forwarded-header trust without documenting and testing the real proxy/network topology. Public rate limits should combine IP with another appropriate factor such as phone number or guest session ID.

### 13.4 Flyway and existing databases

- Flyway is the only mechanism for applying production schema changes; applied migration files are immutable.
- Do not copy a database dump directly into a migration. Remove database creation/selection, destructive dump statements, dump metadata, fixed `AUTO_INCREMENT` values, definers, and transactional/test data first.
- Static reference data may be migrated, but Flyway must never seed a production user with a fixed/default password.
- Before baselining an existing database: create and verify a backup, compare the actual schema/reference data with the intended migrations, test on a restored copy, and record the baseline version selected for that environment.
- Never assume all existing databases have the same baseline version. The next migration must have a version greater than every object represented by that environment's baseline.
- New/CI databases must be tested from an empty MySQL schema with Flyway applying the complete migration chain. Existing-database upgrade tests must also be performed before production rollout.

### 13.5 Production-safe configuration

- Base configuration is production-safe by default: SQL logging off, Swagger off, health details hidden, Open Session in View off, and circular references disabled.
- Development-only settings belong in `application-dev.yml` with explicit `spring.config.activate.on-profile: dev`; production must explicitly use the `prod` profile and never activate `dev`.
- Public Actuator exposure is limited to `health` and `info`, without internal health details. Other management endpoints must remain unexposed or be restricted to an internal/admin management plane.
- Swagger/OpenAPI UI is disabled by default and in production.
- Never enable `spring.main.allow-circular-references`; refactor dependency cycles instead.

### 13.6 Refresh-token concurrency

- Functional interceptors must not keep request state through `this`. Shared refresh state belongs in `AuthService`.
- Concurrent 401 responses must share one refresh request. Clean the shared state with `finalize()` before `shareReplay({ bufferSize: 1, refCount: false })`.
- Requests under `/api/v1/auth/**`, especially the refresh endpoint, must never trigger recursive refresh handling.
- Tests must verify one refresh request for concurrent 401 responses, successful retries with the new access token, cleanup after failure, and absence of refresh loops.

### 13.7 Cache correctness

- A cache key must include every normalized input that can change a response. Cache-key generation and repository queries must use the same canonical defaults and normalization logic.
- Product-list keys include category, descendant inclusion, brand, supplier when applicable, visibility/status, normalized keyword, price range, deterministically sorted attribute filters, page, size, sort field, and sort direction.
- Sort fields use an allowlist. Keep public and admin query methods separate where practical; never cache an admin query merely because its status is `ACTIVE`.
- A missing cache is preferable to a cache with possible key collisions or authorization leakage.
- Cache tests cover distinct filters, cache hits, write eviction, and Redis-unavailable fallback. Never cache cart, order, inventory quantity, or user-sensitive responses.

### 13.8 CI and encoding enforcement

- Required CI commands must not use `--if-present`. Lint checks both TypeScript and Angular templates and must fail CI on errors.
- Do not add disable comments, skip tests, weaken assertions, or relax rules solely to make CI pass. New build warnings must be fixed or explicitly documented with rationale.
- Java, TypeScript, HTML, YAML, and SQL source files use UTF-8. Do not mass-replace suspected mojibake; inspect and repair each damaged string.
- Tests for Vietnamese JSON responses verify JSON-compatible content and a correctly decoded UTF-8 body, not an exact `charset` parameter.

## 14. What the agent must NOT do

- Do not modify a production table's structure without a new migration file.
- Do not hardcode secrets (JWT secret, DB password, Redis password) — always use `application.yml` + environment variables, or GitHub Actions secrets in CI.
- Do not skip `@PreAuthorize` checks on admin APIs, even temporarily for testing.
- Do not cache mutable, real-time-critical data (inventory quantity, cart, order status).
- Do not delete old, already-applied migration files.
- Do not use field injection (`@Autowired` on a field) in Java, or manual `new Service()` instantiation in Angular — see sections 6.6 and 7.4.
- Do not disable, skip, or work around a failing CI check instead of fixing the underlying issue.

## 15. Roadmap (current phase → later)

**Phase 1 (current — do this first):**
1. Set up the Spring Boot project, connect MySQL via Flyway using `database_ban_may_tinh.sql`.
2. Set up the Redis connection + `CacheConfig` (per section 5.1).
3. Generate JPA entities + repositories for `Category`, `Brand`, `Product`, `ProductVariant`.
4. Build basic CRUD REST APIs for these 4 entities, with `@Cacheable`/`@CacheEvict` wired in on Product endpoints.
5. Set up the Angular project scaffold with `core`/`shared`/`features` structure, build simple admin CRUD screens (list/create/edit/delete) for Category, Brand, Product.
6. Write tests confirming cache eviction works correctly on update/delete.

**Phase 2 (after Phase 1 is stable):**
7. Auth module (register/login/JWT refresh) — required before any user-specific feature.
8. Product attributes (EAV) + dynamic spec filtering.
9. Cart → Order flow with inventory deduction transaction (no caching on these — real-time accuracy required).
   - **Payment: no real gateway integration for now.** Only `cod` and a manually-declared "bank transfer" option are supported — `payment_status` moves `unpaid → paid` via a manual "Confirm payment" action (Admin, or auto for COD), not a real VNPay/Momo/ZaloPay integration with callbacks/webhooks. Do not build gateway integration speculatively; revisit only when there's a real need to accept online payments.
10. Discount codes, warehouse/inventory management.
11. Admin dashboard/statistics, news, banners.
