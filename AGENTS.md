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
│   │   ├── entity/          # JPA entities, grouped by domain packages
│   │   │   ├── brand/       # Brand
│   │   │   ├── category/    # Category, CategoryStatus, Converters
│   │   │   ├── product/     # Product, ProductVariant, ProductImage, Converters
│   │   │   ├── supplier/    # Supplier, SupplierStatus, Converters
│   │   │   └── user/        # User, Role, Permission, AuthToken (Phase 2)
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── service/
│   │   │   └── impl/
│   │   ├── mapper/          # DTO Mapper (or static mappers in Response DTOs)
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
- **Cache Bloat Prevention**: With dynamic multi-criteria search filters (keyword, category, brand, supplier, status), avoid creating infinite permutations of cached keys. Prefer caching default catalog pages / common listings, or set short TTL to prevent memory bloating.
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

- Backend: JUnit 5 + Mockito for the service layer; `@SpringBootTest` + Testcontainers (MySQL + Redis containers) for integration tests, including cache-eviction-on-write behavior.
- Frontend: Jasmine/Karma for unit tests; Cypress/Playwright for e2e on core flows once they exist.
- Whenever Redis caching is added to a CRUD endpoint, write a test verifying that an update/delete actually evicts the cache (stale reads are a common bug class here).

## 11. Git & commit conventions

- Branches: `main` (production), `develop` (staging), `feature/<name>`, `fix/<name>`.
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`.
  - Example: `feat(product): add Redis caching for product detail endpoint`
- PRs must describe: purpose, affected DB tables (if any), how it was tested, and whether cache eviction was verified.

## 12. Continuous Integration (GitHub Actions)

CI configuration lives in `.github/workflows/`. Every PR into `main` or `develop` must pass CI before merge — do not bypass or skip failing checks by disabling a workflow step.

### 12.1 Required workflows
- **`backend-ci.yml`** — triggers on changes under `demo/**`:
  1. Checkout, set up JDK 21.
  2. `mvn -B verify` (compiles, runs unit tests, runs Testcontainers-based integration tests).
  3. Fail the build on any test failure — do not mark tests `@Disabled` to force a green build.
- **`frontend-ci.yml`** — triggers on changes under `frontend/**`:
  1. Checkout, set up Node LTS, `npm ci` (never `npm install` in CI — lockfile must be respected exactly).
  2. `npm run lint`.
  3. `npm run test -- --watch=false --browsers=ChromeHeadless`.
  4. `npm run build -- --configuration production` to catch build-time errors early.
- Use `paths:` filters so backend changes don't trigger the frontend workflow and vice versa — keeps CI fast and cheap.

### 12.2 Rules for the agent
- Any new feature that adds a Maven dependency, npm package, or new test command must also update the corresponding workflow file if the existing steps no longer cover it (e.g. adding Testcontainers requires Docker-in-Docker support already assumed in `backend-ci.yml` — flag this to the user if a runner change is needed).
- Never commit code that fails `mvn verify` or `npm run lint` locally — CI is a safety net, not a substitute for running checks before pushing.
- Secrets used in CI (DB password for integration tests, etc.) must be referenced via GitHub Actions `secrets.*` context — never hardcoded in the workflow YAML.
- When adding a new Redis-dependent test (per section 10), ensure the workflow spins up a Redis service container (`services: redis: image: redis:7-alpine`) alongside MySQL/Testcontainers, or the test will fail in CI even if it passes locally.
- Branch protection on `main`/`develop` requires both `backend-ci` and `frontend-ci` (when relevant paths changed) to pass, plus at least one review approval — do not suggest merging around this.

## 13. What the agent must NOT do

- Do not modify a production table's structure without a new migration file.
- Do not hardcode secrets (JWT secret, DB password, Redis password) — always use `application.yml` + environment variables, or GitHub Actions secrets in CI.
- Do not skip `@PreAuthorize` checks on admin APIs, even temporarily for testing.
- Do not cache mutable, real-time-critical data (inventory quantity, cart, order status).
- Do not delete old, already-applied migration files.
- Do not use field injection (`@Autowired` on a field) in Java, or manual `new Service()` instantiation in Angular — see sections 6.6 and 7.4.
- Do not disable, skip, or work around a failing CI check instead of fixing the underlying issue.

## 14. Roadmap (current phase → later)

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
10. Discount codes, warehouse/inventory management.
11. Admin dashboard/statistics, news, banners.