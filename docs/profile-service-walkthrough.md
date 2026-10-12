# Profile: từ nghiệp vụ đến code (walkthrough)

> Viết ngày 2026-10-12, dựa trên code thật sau khi hoàn thiện `profile-service`.
> Đọc theo thứ tự: **1. Nghiệp vụ → 2. Storyboard UI → 3. Backend từng bước → 4. Phía app → 5. Tự kiểm tra**.
> Tài liệu kế hoạch và nhật ký triển khai nằm ở [profile-service-plan.md](profile-service-plan.md).

---

## 1. Business logic: Profile là gì và tuân theo luật nào

### 1.1. Vai trò trong app

```
Hồ sơ cơ thể (6 trường)  ──►  CalculateNutritionTargetUseCase  ──►  Mục tiêu mỗi ngày
giới tính, tuổi, cao,         BMR (Mifflin–St Jeor) × hệ số vận động     kcal + protein/carb/fat
nặng, mức vận động, mục tiêu   ± điều chỉnh theo mục tiêu                 (Goal, Dashboard, History…)
```

Không có hồ sơ thì app **không biết mục tiêu calo**, nên Dashboard không có ý nghĩa. Vì vậy Profile đứng đầu chuỗi: Đăng ký → Onboarding → Goal → Dashboard.

### 1.2. Sáu luật nghiệp vụ

| # | Luật | Hệ quả trong code |
|---|---|---|
| R1 | Mỗi user có **đúng một** hồ sơ | `user_id` là khoá chính của `body_profiles` |
| R2 | Hồ sơ được tạo ở **Onboarding, sau khi đăng ký**, nên có thể **chưa tồn tại** (user bỏ dở) | `GET` trả `404` là trạng thái bình thường, không phải lỗi |
| R3 | Giá trị hợp lệ: tuổi 14–90, cao 120–220 cm, nặng 30–200 kg, 3 trường enum | Kiểm tra ở 3 nơi: app (`BodyProfile.init`), API (`@Min/@Max`), DB (`CHECK`) |
| R4 | **Chỉ chủ hồ sơ** được đọc và sửa | userId lấy từ token, **không bao giờ** lấy từ URL hay body |
| R5 | Lưu là **ghi đè** (upsert). Lần đầu và các lần sửa dùng chung một thao tác | Chỉ có `PUT /me`, không có `POST` |
| R6 | Server lưu **dữ liệu thô**, app tự tính TDEE | Server không có công thức TDEE, nên không bị lệch công thức giữa hai nơi |

> R6 là một đánh đổi. Sau này `nutrition-service` cần mục tiêu calo để tổng kết ngày, lúc đó sẽ phải quyết định lại xem ai là người tính.

### 1.3. Trạng thái hồ sơ trên máy (`ProfileStatus`)

App phải trả lời được câu hỏi: *"Hồ sơ đang hiện trên màn hình có phải của user này thật không?"*

| Trạng thái | Nghĩa | Màn hình phản ứng thế nào |
|---|---|---|
| `LOADING` | Chưa đăng nhập, hoặc đang tải | Nút đăng nhập tiếp tục quay; Dashboard hiện loading; form sửa hồ sơ bị ẩn |
| `MISSING` | Đã đăng nhập nhưng server trả `404` (bỏ dở onboarding) | Sau đăng nhập, đưa về **Onboarding** |
| `READY` | Đã có hồ sơ thật | Mọi màn tính theo hồ sơ này |
| `FAILED` | Tải lỗi (mất mạng, server sập) | Dashboard hiện "Chưa tải được hồ sơ" + nút **Thử lại** |

Khai báo ở `composeApp/.../domain/model/BodyProfile.kt` (`enum class ProfileStatus`).

---

## 2. Storyboard: thao tác trên UI

### Luồng A: Đăng ký mới (happy path)

```
┌──────────────┐  Đăng ký   ┌──────────────────┐  "Tính mục tiêu"  ┌─────────────┐  Tiếp  ┌─────────────┐
│ Auth         │──────────►│ Onboarding       │─────────────────►│ Goal        │──────►│ Dashboard   │
│ email + mk   │  POST      │ chọn giới tính,  │  PUT /profile/me  │ TDEE, kcal, │       │ mục tiêu    │
│              │  /register │ +/- tuổi/cao/nặng│  200 → READY      │ macro       │       │ của user    │
└──────────────┘  (8081)    │ vận động, mục tiêu│  (8082)          └─────────────┘       └─────────────┘
                            └──────────────────┘
```

- **A-lỗi:** `PUT` thất bại (mất mạng, `401`…) → app hiện snackbar *"Chưa lưu được hồ sơ…"* và **ở lại** Onboarding, dữ liệu đã nhập vẫn còn.

### Luồng B: Đăng nhập, đã có hồ sơ

```
App                         identity-service:8081        profile-service:8082
 │ POST /auth/login ───────────►│
 │◄─────── 200 {accessToken} ───│
 │ session = user  ──► RemoteProfileRepository tự phản ứng:
 │ GET /profile/me  (Bearer token) ──────────────────────────►│ kiểm tra chữ ký, exp, uid
 │◄──────────────────────────────────────────── 200 hồ sơ ────│
 │ status = READY → AuthViewModel thôi chờ → Dashboard
```

Trong lúc chờ, nút đăng nhập **vẫn quay**, để Dashboard không hiện mục tiêu mặc định rồi mới nhảy sang số thật.

### Luồng C: Đăng nhập, chưa có hồ sơ
`GET` trả `404` → `MISSING` → đưa về **Onboarding** thay vì Dashboard.

### Luồng D: Đăng nhập khi profile-service đang sập
`GET` lỗi → `FAILED` → vẫn vào Dashboard nhưng hiện *"Chưa tải được hồ sơ"* + **Thử lại**. Bấm Thử lại sẽ gọi `refresh()`; server vẫn sập thì vẫn hiện lỗi, không crash.

### Luồng E: Sửa hồ sơ
Dashboard → **Hồ sơ** (Onboarding ở chế độ `isEditing = true`):
- Form **bị ẩn** cho tới khi `READY`. Nếu không ẩn, user có thể bấm lưu khi form đang chứa giá trị mặc định, và `PUT` sẽ **ghi đè hồ sơ thật bằng giá trị mặc định**.
- `READY` thì nạp hồ sơ vừa tải vào form → user sửa → `PUT` → về Goal.

### Luồng F: Đăng xuất
Màn **Hồ sơ** → "Đăng xuất" → `authRepository.logout()` → `session = null` → profile repository reset về `LOADING` + `Default` → điều hướng về Auth và **xoá cả back stack** (bấm Back không quay lại được Dashboard).

---

## 3. Backend: triển khai từ đầu, từng bước

Thứ tự đi từ **hợp đồng → hạ tầng → database → code từ trong ra ngoài → test**. Mỗi file chỉ phụ thuộc vào những file đã có trước nó.

### Bước 0: Chốt hợp đồng giữa 2 service (làm TRƯỚC khi viết code)

Profile-service **không gọi** identity-service. Nó chỉ tin vào token, nên hai bên phải thống nhất 3 điều:

| Điều khoản | Giá trị | Sai thì chuyện gì xảy ra |
|---|---|---|
| Thuật toán ký | **HS256**, cố định ở cả hai phía | Mọi token bị `401` mà không báo lý do |
| Secret | Cùng `JWT_SECRET` | Như trên |
| Claim mang userId | `uid` (UUID) | Không biết token là của ai |

Bài học thật đã gặp: jjwt **tự chọn thuật toán theo độ dài secret** (secret 61 byte thì ra HS384). Cách sửa là cố định ở `identity-service/.../service/JwtService.java`:

```java
.signWith(signingKey, Jwts.SIG.HS256)
```

### Bước 1: Module và hạ tầng

| File | Việc |
|---|---|
| `settings.gradle` | `include 'profile-service'` |
| `docker-compose.yml` | Thêm `profile-db` (postgres:18-alpine, `127.0.0.1:5434`, volume riêng, healthcheck) |
| `.env.example` | Thêm `PROFILE_DB_*`, `PROFILE_SERVICE_PORT`, và ghi chú rằng `JWT_SECRET` dùng chung |

**Vì sao database riêng:** đây là luật "database per service". Nhưng nó kéo theo hệ quả: `body_profiles.user_id` **không có khoá ngoại** tới `users`, vì hai bảng nằm ở hai database khác nhau. Xoá user thì hồ sơ không tự xoá. Bài toán đó sẽ giải sau bằng event.

### Bước 2: `profile-service/build.gradle`

Dependency đáng chú ý nhất:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
```

Identity-service tự viết `JwtAuthenticationFilter`. Profile-service **không cần làm vậy**: "Resource Server" là vai trò chuẩn của Spring Security cho *"service nhận token do nơi khác cấp"*. Nó lo sẵn việc đọc header `Bearer`, kiểm tra chữ ký và hạn dùng, và trả `401` đúng chuẩn (kèm `WWW-Authenticate`).

Test dùng H2 (`testRuntimeOnly 'com.h2database:h2'`) để chạy được mà không cần Docker.

### Bước 3: `application.properties`

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
server.port=${PROFILE_SERVICE_PORT:8082}
spring.datasource.url=jdbc:postgresql://127.0.0.1:${PROFILE_DB_PORT:5434}/${PROFILE_DB_NAME:profile_db}
spring.jpa.hibernate.ddl-auto=validate          # Hibernate chỉ KIỂM TRA schema, Flyway mới tạo bảng
spring.jpa.open-in-view=false
visionfit.jwt.secret=${JWT_SECRET:...}          # phải bằng secret của identity-service
spring.mvc.problemdetails.enabled=true          # lỗi trả theo chuẩn RFC 9457, khỏi viết ExceptionHandler
```

### Bước 4: Migration `db/migration/V1__create_body_profiles_table.sql`

```sql
CREATE TABLE body_profiles (
    user_id        UUID PRIMARY KEY,                        -- R1: 1 user : 1 hồ sơ, không có FK
    sex            VARCHAR(10) NOT NULL CHECK (sex IN ('MALE','FEMALE')),
    age_years      INTEGER     NOT NULL CHECK (age_years BETWEEN 14 AND 90),   -- R3
    height_cm      INTEGER     NOT NULL CHECK (height_cm BETWEEN 120 AND 220),
    weight_kg      INTEGER     NOT NULL CHECK (weight_kg BETWEEN 30 AND 200),
    activity_level VARCHAR(20) NOT NULL CHECK (activity_level IN ('SEDENTARY','LIGHT','MODERATE','ACTIVE')),
    goal           VARCHAR(20) NOT NULL CHECK (goal IN ('FAT_LOSS','MUSCLE_GAIN')),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

- Dùng `INTEGER` chứ không dùng `SMALLINT`: field Java là `int`, mà `ddl-auto=validate` đòi kiểu phải khớp chính xác, nếu không service sẽ không khởi động.
- `CHECK` là **lớp bảo vệ cuối cùng**: kể cả khi code có bug, database vẫn không nhận dữ liệu sai.

### Bước 5: Entity `domain/BodyProfile.java`

Những điểm chính:
- **3 enum lồng bên trong** (`Sex`, `ActivityLevel`, `FitnessGoal`), được lưu bằng `@Enumerated(EnumType.STRING)`. Lưu theo **tên** thay vì số thứ tự, vì thêm một giá trị enum vào giữa sẽ làm lệch toàn bộ dữ liệu cũ. Tên enum cũng chính là định dạng JSON mà app gửi lên, nên **đổi tên một enum là app hỏng**.
- `@Id` **được gán, không tự sinh**: `new BodyProfile(userId)` với userId lấy từ token.
- `@PrePersist @PreUpdate void touch()` tự cập nhật `updatedAt`.
- Chỉ có **một hàm `update(...)`** thay cho 6 setter, vì hồ sơ luôn được ghi trọn cả 6 trường (R5).
- Có constructor `protected BodyProfile()` vì JPA cần nó.

### Bước 6: `repository/BodyProfileRepository.java`

`JpaRepository<BodyProfile, UUID>` là đủ. `findById(userId)` dùng chính khoá chính, nên không cần viết thêm query nào.

### Bước 7: DTO `dto/ProfileDto.java`

```java
public record ProfileDto(
        @NotNull Sex sex,
        @NotNull @Min(14) @Max(90) Integer ageYears,   // Integer (không dùng int) để @NotNull bắt được trường hợp thiếu field
        ...
) {
    public static ProfileDto from(BodyProfile profile) { ... }
}
```

- **Một record dùng cho cả request lẫn response**, vì hai chiều có cùng hình dạng.
- **Không có `userId`** trong DTO, đây chính là luật R4.
- Vì sao không trả thẳng Entity ra API: tránh lộ field nội bộ (`updatedAt`, `userId`), và đổi bảng thì không làm vỡ API.

### Bước 8: `config/SecurityConfig.java` (phần quan trọng nhất)

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
        .anyRequest().authenticated())
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

```java
@Bean
JwtDecoder jwtDecoder(@Value("${visionfit.jwt.secret}") String secret) {
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),                               // exp, nbf (lệch giờ tối đa 60 giây)
            new JwtClaimValidator<String>("uid", SecurityConfig::isUuid) // bắt buộc có uid là UUID
    ));
    return decoder;
}
```

Một request đi qua những bước sau:

```
PUT /api/v1/profile/me  + Authorization: Bearer eyJ...
  │
  ├─ BearerTokenAuthenticationFilter   lấy token ra khỏi header
  ├─ JwtDecoder                         chữ ký HS256 đúng? còn hạn? có uid?   ── không → 401
  ├─ SecurityContext = Jwt              token hợp lệ được gắn vào request
  ├─ ProfileController.put              @AuthenticationPrincipal Jwt jwt
  ├─ @Valid ProfileDto                  sai ràng buộc → 400 (problem detail)
  ├─ ProfileService.upsert              @Transactional
  └─ BodyProfileRepository.save → profile-db
```

### Bước 9: `service/ProfileService.java`

```java
@Transactional(readOnly = true)
public Optional<ProfileDto> get(UUID userId) {
    return repository.findById(userId).map(ProfileDto::from);
}

@Transactional
public ProfileDto upsert(UUID userId, ProfileDto dto) {
    BodyProfile profile = repository.findById(userId).orElseGet(() -> new BodyProfile(userId)); // chưa có thì tạo mới
    profile.update(dto.sex(), dto.ageYears(), ...);                                            // có rồi thì ghi đè
    return ProfileDto.from(repository.save(profile));
}
```

- `Optional` thể hiện đúng luật R2: "có thể chưa có hồ sơ".
- **Điểm yếu đã biết:** nếu cùng một user gửi **2 lần lưu đầu tiên cùng lúc**, cả hai đều thấy "chưa có" và cùng `INSERT`, một request sẽ lỗi `500`. App chỉ gửi một lần lưu mỗi lúc nên tạm chấp nhận. Cách sửa: `INSERT ... ON CONFLICT` hoặc retry.

### Bước 10: `controller/ProfileController.java`

```java
@RequestMapping("/api/v1/profile/me")
...
@GetMapping
public ResponseEntity<ProfileDto> get(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.of(profileService.get(userId(jwt)));   // Optional rỗng → 404 tự động
}

@PutMapping
public ProfileDto put(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileDto body) { ... }

private static UUID userId(Jwt jwt) { return UUID.fromString(jwt.getClaimAsString("uid")); }
```

**Vì sao đường dẫn là `/me` chứ không phải `/{userId}`:** với `/{userId}`, user A chỉ cần đổi id trên URL là đọc được hồ sơ của user B. Lỗ hổng này gọi là **IDOR**. Dùng `/me` thì không có id nào để sửa cả.

### Bước 11: Test `src/test/.../ProfileApiTest.java` (MockMvc + token thật ký bằng secret)

| Test | Chứng minh điều gì |
|---|---|
| `rejectsMissingForgedExpiredAndUidlessTokens` | Thiếu token, sai chữ ký, hết hạn, thiếu `uid` → đều trả `401` |
| `notFoundUntilSavedThenReturnsTheSavedProfile` | R2 + R5: `404` → `PUT` → `GET` ra đúng dữ liệu; `PUT` lần 2 thì ghi đè |
| `usersOnlySeeTheirOwnProfile` | R4: user B không thấy hồ sơ của user A |
| `rejectsValuesOutsideTheAppsRanges` | R3: tuổi 10 → `400` |

Bẫy đã gặp: `JwtTimestampValidator` cho phép lệch giờ **60 giây**, nên muốn test token hết hạn thì phải lùi `exp` quá mốc đó.

### Bước 12: Chạy thật

```bash
docker compose up -d                       # identity-db + profile-db
./gradlew :identity-service:bootRun        # terminal 1
./gradlew :profile-service:bootRun         # terminal 2
```

Sau đó đăng ký lấy token, rồi `GET /api/v1/profile/me` (`404`) → `PUT` (`200`) → `GET` (`200`).

---

## 4. Phía app: Domain → Data → DI → Presentation

| Tầng | Thay đổi | Lý do |
|---|---|---|
| **Domain** | `ProfileStatus`; `ProfileRepository` có thêm `status`, `refresh()`, và `saveProfile(...)` trả về `Boolean` | Bản mock luôn có sẵn hồ sơ, nên UI chưa từng phải xử lý "đang tải / chưa có / lỗi" |
| **Data** | `data/remote/dto/ProfileDtos.kt`: dùng thẳng enum của domain, vì tên enum trùng với định dạng server cần | Không phải viết mapper cho enum |
| **Data** | `data/repository/RemoteProfileRepository.kt`: `session.collectLatest { reset; if (user != null) download() }` | Đăng nhập thì **tự tải** hồ sơ; đăng xuất thì reset; `collectLatest` huỷ lần tải cũ nếu session đổi giữa chừng |
| **Data** | `bearerAuth(token)` trên mỗi `GET`/`PUT`; `200 → READY`, `404 → MISSING`, còn lại → `FAILED` | Ánh xạ status code của server sang trạng thái nghiệp vụ |
| **DI** | `ApiUrls(identity, profile)`; **một `TokenStore` dùng chung**; mỗi service một `HttpClient` | Auth ghi token vào, profile đọc token ra |
| **Presentation** | `AuthViewModel.destinationAfterLogin()` chờ `status != LOADING` | Luồng B và C |
| | Onboarding: effect `SaveFailed` (A-lỗi), ẩn form khi đang sửa mà chưa `READY` (E), nút Đăng xuất (F) | |
| | Dashboard: `isLoading`, `isProfileFailed` + `RetryProfileClicked → refresh()` | Luồng D |

Test: `RemoteProfileRepositoryTest` (7 test, dùng `MockEngine`) và `ProfileGateTests` (5 test, dùng `FakeProfileRepository`).

---

## 5. Tự kiểm tra (trả lời được hết là đã hiểu)

1. Vì sao `GET` trả `404` lại **không phải** lỗi trong nghiệp vụ này?
2. Nếu đổi API thành `GET /api/v1/profile/{userId}`, kẻ xấu sẽ khai thác thế nào?
3. Profile-service **không gọi** identity-service, vậy làm sao nó biết token là thật?
4. Nếu identity-service ký HS384 còn profile-service đòi HS256, triệu chứng sẽ là gì?
5. Vì sao form sửa hồ sơ phải bị ẩn cho tới khi `READY`? Nếu không ẩn thì dữ liệu nào bị hỏng?
6. Xoá một user trong `identity-db`, chuyện gì xảy ra với dòng của họ trong `profile-db`? Sau này sửa bằng cách nào?
7. Vì sao lưu enum bằng `EnumType.STRING` chứ không dùng `ORDINAL`?

---

## 6. Còn nợ (biết rõ, chưa làm)

| Việc | Ảnh hưởng hiện tại |
|---|---|
| App chưa **tự refresh token khi gặp `401`** | Sau 1 giờ (hạn của access token), tải và lưu hồ sơ đều thất bại; phải đăng nhập lại |
| Token chưa được lưu xuống máy | Tắt app là phải đăng nhập lại |
| Xoá user chưa xoá hồ sơ | Dữ liệu mồ côi trong `profile-db` (cần event khi có message broker) |
| Hai lần lưu đầu tiên chạy đồng thời gây `500` | Hiếm gặp; sửa bằng `ON CONFLICT` |
| History, Goal, Review chưa có màn lỗi riêng | Nếu tải hồ sơ lỗi, các màn này tính theo hồ sơ mặc định |
| Chưa có API Gateway | App phải biết 2 cổng (8081, 8082) |
