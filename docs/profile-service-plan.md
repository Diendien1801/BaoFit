# Kế hoạch: `profile-service` (Hồ sơ cơ thể)

> Ngày lập: 2026-10-12. Phạm vi: repo backend `visionfit-backend` và app `BaoFit`.
> Quyết định của user: **tách riêng `profile-service` với database riêng** (không đặt trong `identity-service`), để trải nghiệm phần khó của microservice.

---

## 1. Vì sao chọn Profile

| Ứng viên | Màn hình dùng tới | Độ khó | Khái niệm mới |
|---|---|---|---|
| **Profile** (`ProfileRepository`) | Onboarding, Goal, Dashboard, History, Review | ⭐⭐ (vì tách service) | Endpoint cần đăng nhập, service khác tự kiểm tra JWT, database riêng, app gọi 2 service |
| Meal (`MealRepository`) | Dashboard, History, Review | ⭐⭐⭐ | Truy vấn theo khoảng ngày, quan hệ 1-nhiều, phân trang |
| Analysis (`MealAnalysisRepository`) | Camera, Analysis | ⭐⭐⭐⭐⭐ | Upload ảnh, job bất đồng bộ, AI, polling |

Profile đứng đầu chuỗi phụ thuộc (Đăng ký → Onboarding → Goal → Dashboard). Dữ liệu đơn giản (1 dòng mỗi user), nên ta tập trung được vào phần khó thật sự là **giao tiếp giữa các service**.

---

## 2. Kiến trúc

```
                 ┌──────────────────────┐   POST /api/v1/auth/login
   App (KMP) ───►│ identity-service:8081│──► identity-db:5433 (users, refresh_tokens)
       │         └──────────┬───────────┘
       │                    │ ký access token: HS256, sub = email, uid = userId
       │                    ▼
       │   GET/PUT /api/v1/profile/me   (Authorization: Bearer <token>)
       └────────►┌──────────────────────┐
                 │ profile-service:8082 │──► profile-db:5434 (body_profiles)
                 └──────────────────────┘
                 tự kiểm tra chữ ký bằng JWT_SECRET dùng chung,
                 KHÔNG gọi identity-service, KHÔNG đọc identity-db
```

**Hợp đồng giữa 2 service (quan trọng nhất):**
- Thuật toán ký **cố định là HS256** ở cả hai phía. Trước đây jjwt tự chọn thuật toán theo độ dài secret (secret 61 ký tự thì chọn HS384), nên phía nhận không thể đoán được.
- Hai service dùng chung `JWT_SECRET`.
- Claim `uid` mang userId (UUID). Thiếu `uid` thì token bị coi là không hợp lệ và trả 401.

**Hệ quả của "mỗi service một database":**
- `body_profiles.user_id` **không có khóa ngoại** tới `users`, vì hai bảng nằm ở 2 database khác nhau.
- Xóa user thì profile **không tự xóa**. Đây là bài toán đồng bộ dữ liệu giữa service, sẽ giải quyết bằng event khi có message broker (ngoài phạm vi lần này).
- Access token còn hạn thì profile-service vẫn chấp nhận, kể cả khi user đã logout. Đó là đánh đổi của JWT stateless. Token chỉ sống 1 giờ.

---

## 3. API

| Method | Path | Body | Phản hồi |
|---|---|---|---|
| `GET` | `/api/v1/profile/me` | | `200` ProfileDto · `404` chưa có profile · `401` thiếu hoặc sai token |
| `PUT` | `/api/v1/profile/me` | ProfileDto | `200` ProfileDto (upsert) · `400` sai ràng buộc · `401` |

```json
{ "sex": "MALE", "ageYears": 25, "heightCm": 172, "weightKg": 68,
  "activityLevel": "MODERATE", "goal": "FAT_LOSS" }
```

- **Không nhận userId từ URL hay body.** User nào thì lấy từ claim `uid` trong token. Cách này chặn được lỗ hổng IDOR (sửa id để đọc dữ liệu người khác).
- **`PUT` là idempotent**: gửi lại 2 lần vẫn cho cùng kết quả. Onboarding lần đầu và màn sửa hồ sơ dùng chung một lệnh gọi.
- Ràng buộc khớp với `BodyProfile` trong app: tuổi 14..90, chiều cao 120..220 cm, cân nặng 30..200 kg.

### Database (`profile-db`, Flyway `V1`)

```sql
CREATE TABLE body_profiles (
    user_id        UUID PRIMARY KEY,          -- 1 user : 1 profile; không có FK vì users ở database khác
    sex            VARCHAR(10) NOT NULL CHECK (sex IN ('MALE','FEMALE')),
    age_years      INTEGER     NOT NULL CHECK (age_years BETWEEN 14 AND 90),
    height_cm      INTEGER     NOT NULL CHECK (height_cm BETWEEN 120 AND 220),
    weight_kg      INTEGER     NOT NULL CHECK (weight_kg BETWEEN 30 AND 200),
    activity_level VARCHAR(20) NOT NULL CHECK (activity_level IN ('SEDENTARY','LIGHT','MODERATE','ACTIVE')),
    goal           VARCHAR(20) NOT NULL CHECK (goal IN ('FAT_LOSS','MUSCLE_GAIN')),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Có 2 lớp kiểm tra: Bean Validation (`@Min`/`@Max`) để trả `400` dễ hiểu, và `CHECK` trong database là lớp bảo vệ cuối cùng.

---

## 4. Lỗ hổng phía app khi bỏ Mock

| # | Lỗ hổng | Cách xử lý |
|---|---|---|
| G1 | Không có thao tác tải profile về. User cũ đăng nhập sẽ thấy `Default` | `RemoteProfileRepository` theo dõi `authRepository.session`: có user thì GET, đăng xuất thì reset về `Default`. ViewModel không phải sửa |
| G2 | `saveProfile` không báo lỗi, Onboarding vẫn chuyển màn khi lỗi mạng | `saveProfile(...): Boolean`. Lỗi thì gửi effect `SaveFailed` và hiện snackbar |
| G3 | `HttpClient` chưa gửi token | `bearerAuth(accessToken)` trên mỗi request của profile |
| G4 | `TokenStore` đang được tạo ngay trong dòng khởi tạo `RemoteAuthRepository` | Đưa ra thành `val` trong `AppContainer` để hai repository dùng chung |
| G5 | App chỉ có 1 `apiBaseUrl` | `ApiUrls(identity, profile)`. Khi có Gateway thì hai giá trị trỏ về cùng một URL |

---

## 5. Các bước triển khai

### Phase A: Backend (`visionfit-backend`)

| Bước | Việc | File |
|---|---|---|
| A1 | Cố định HS256 khi ký token | `identity-service/.../JwtService.java` |
| A2 | Thêm module và hạ tầng: `include 'profile-service'`, `profile-db` trong compose, biến môi trường | `settings.gradle`, `docker-compose.yml`, `.env.example` |
| A3 | `build.gradle` + `application.properties` (port 8082, DB 5434, cùng `JWT_SECRET`) | `profile-service/` |
| A4 | Migration `V1__create_body_profiles_table.sql` | `db/migration/` |
| A5 | Entity `BodyProfile` + 3 enum + `BodyProfileRepository` | `domain/`, `repository/` |
| A6 | `ProfileDto` (record + Bean Validation) | `dto/` |
| A7 | `SecurityConfig`: OAuth2 Resource Server, `NimbusJwtDecoder` dùng secret HS256, validator bắt buộc có `uid` | `config/` |
| A8 | `ProfileService` (`get`, `upsert` có `@Transactional`) + `ProfileController` (`@AuthenticationPrincipal Jwt`) | `service/`, `controller/` |
| A9 | Test MockMvc với token thật ký bằng secret: 401 thiếu token · 401 sai chữ ký · 401 thiếu `uid` · 404 lần đầu · PUT rồi GET · 400 tuổi 10 · 2 user không thấy dữ liệu của nhau | `src/test/` |
| A10 | Chạy thật: compose up 2 database, chạy 2 service, dùng `curl` đăng ký rồi PUT/GET profile | |

### Phase B: App (`BaoFit`), theo thứ tự Domain → Data → DI → Presentation

| Bước | Việc | File |
|---|---|---|
| B1 | `saveProfile(...): Boolean` | `domain/repository/Repositories.kt` |
| B2 | Mock trả `true`. Onboarding: thêm effect `SaveFailed`, Route hiện snackbar | `MockProfileRepository`, `onboarding/*` |
| B3 | `ProfileDto` (`@Serializable`, dùng thẳng enum của domain, vì kotlinx.serialization tự tuần tự hóa enum theo tên) | `data/remote/dto/ProfileDtos.kt` |
| B4 | `ApiUrls` + giá trị theo từng nền tảng (8081/8082) | `data/remote/ApiConfig*.kt` |
| B5 | `RemoteProfileRepository(client, tokenStore, session, scope)` | `data/repository/` |
| B6 | `AppContainer(api: ApiUrls?)`: dùng chung `TokenStore`, mỗi service một `HttpClient`. `api == null` thì dùng Mock | `di/AppContainer.kt`, `App.kt` |
| B7 | Test bằng `MockEngine`: gửi Bearer · đăng nhập thì tải về · 404 giữ `Default` · đăng xuất thì reset · PUT thành công và thất bại · không có token thì không gọi mạng | `commonTest/.../data/RemoteProfileRepositoryTest.kt` |
| B8 | Thử toàn luồng: đăng ký → onboarding (30 tuổi / 80 kg) → tắt app → đăng nhập lại → Dashboard đúng mục tiêu | |

### Phase C: để sau

- **API Gateway** (Spring Cloud Gateway, cổng 8080): app chỉ cần 1 URL, gom CORS và rate limit về một chỗ.
- **Refresh token tự động khi gặp 401.** Access token hết hạn sau 1 giờ. Hiện tại sau 1 giờ, profile sẽ tải và lưu thất bại.
- **Lưu token xuống máy** (Keychain / EncryptedSharedPreferences).
- **Xóa profile khi user bị xóa**: identity phát event, profile nghe và xóa.

---

## 6. Hoàn thành khi

- [x] `:identity-service:test` (2) và `:profile-service:test` (4) xanh
- [x] `:composeApp:jvmTest` xanh (56 test, trong đó có 6 test mới của `RemoteProfileRepositoryTest`). iOS và Android compile được
- [x] A10: `curl` trên Postgres thật: token có `"alg":"HS256"`; không token trả 401; GET đầu trả 404; PUT trả 200; GET trả 200; tuổi 10 trả 400 (problem detail). Dòng dữ liệu nằm trong `profile-db`
- [x] B8 phần dữ liệu: một test tích hợp tạm thời dùng `AppContainer` thật gọi 2 service thật. Đăng ký → lưu → container mới đăng nhập → profile tự tải về → đăng xuất thì reset. Test đã xóa sau khi chạy
- [x] B8 phần giao diện: Compose UI test tạm thời điều khiển giao diện thật với backend thật (xem mục 7b). Đã xóa sau khi chạy

---

## 7. Kết quả triển khai (2026-10-12)

### Backend: `visionfit-backend` (chưa commit)

| File | Thay đổi |
|---|---|
| `identity-service/.../JwtService.java` | `signWith(signingKey, Jwts.SIG.HS256)`: cố định thuật toán |
| `settings.gradle`, `docker-compose.yml`, `.env.example` | Module mới, `profile-db` cổng 5434, biến `PROFILE_*` |
| `profile-service/build.gradle` | Dùng `spring-boot-starter-security-oauth2-resource-server` thay cho tự viết JWT filter |
| `.../config/SecurityConfig.java` | `NimbusJwtDecoder` dùng HS256 + validator mặc định (`exp`, `nbf`) + `JwtClaimValidator("uid", isUuid)` |
| `.../domain/BodyProfile.java` | Entity, 3 enum lồng bên trong, `@PrePersist/@PreUpdate` cập nhật `updated_at` |
| `.../dto/ProfileDto.java` | Một record dùng cho cả request lẫn response, có `@NotNull @Min @Max` |
| `.../service/ProfileService.java` | `get` (readOnly) và `upsert` (`findById` → `orElseGet(new)` → `update` → `save`) |
| `.../controller/ProfileController.java` | `ResponseEntity.of(Optional)` tự trả 404; `userId` lấy từ `jwt.getClaimAsString("uid")` |
| `src/main/resources/db/migration/V1__...sql` | Bảng `body_profiles` có `CHECK` |
| `src/test/.../ProfileApiTest.java` | MockMvc với token thật do `NimbusJwtEncoder` ký |

Những phần cố ý bỏ qua:
- Không viết `GlobalExceptionHandler`. Dùng `spring.mvc.problemdetails.enabled=true` (chuẩn RFC 9457 có sẵn), vì app chỉ đọc status code.
- Hai lần lưu profile đầu tiên chạy đồng thời sẽ tranh nhau khóa chính. Đã ghi chú `ponytail:` trong `ProfileService`.

### App: `BaoFit` (chưa commit)

| File | Thay đổi |
|---|---|
| `domain/repository/Repositories.kt` | `saveProfile(...): Boolean` |
| `data/repository/MockProfileRepository.kt` | `return true` |
| `presentation/onboarding/*` | Effect `SaveFailed`; Route hiện snackbar "Chưa lưu được hồ sơ…" |
| `data/remote/ApiConfig*.kt` | `ApiUrls(identity, profile)`; `expect val devMachineUrl` (mỗi nền tảng chỉ khai báo host, cổng nằm ở commonMain) |
| `data/remote/dto/ProfileDtos.kt` | `ProfileDto` dùng thẳng enum của domain |
| `data/repository/RemoteProfileRepository.kt` | Theo dõi `session` bằng `collectLatest`; GET/PUT có `bearerAuth` |
| `di/AppContainer.kt`, `App.kt` | `api: ApiUrls?`, dùng chung `tokenStore`, mỗi service một `HttpClient` |
| `commonTest/.../RemoteProfileRepositoryTest.kt` | 6 test dùng `MockEngine` |

### Bài học gặp phải khi làm (nên đọc khi review)

1. **jjwt tự chọn thuật toán theo độ dài secret.** Secret mặc định dài 61 byte nên trước đây token được ký HS384. Phía nhận cấu hình HS256 sẽ trả 401 cho mọi token mà không báo lý do. Cách sửa là cố định thuật toán ở cả hai phía.
2. **`advanceUntilIdle()` bỏ qua `backgroundScope`.** Repository theo dõi `session` trong scope nền, nên test phải dùng `runCurrent()`. Lúc đầu test hỏng vì collector chưa hề chạy (`subscriptionCount = 0`).
3. **Hibernate `ddl-auto=validate`:** field kiểu `int` phải đi với cột `INTEGER`. Nếu dùng `SMALLINT` thì service không khởi động được.
4. **Thời gian lệch mặc định 60 giây của `JwtTimestampValidator`:** token hết hạn chưa quá 60 giây vẫn được chấp nhận. Test token hết hạn phải lùi `exp` xa hơn mốc đó.

### 7b. Tích hợp vào giao diện (làm thêm sau khi review)

Bản Mock luôn có sẵn hồ sơ, nên giao diện chưa từng phải xử lý các trường hợp "đang tải / chưa có / lỗi". Để vá, Domain có thêm:

```kotlin
enum class ProfileStatus { LOADING, MISSING, READY, FAILED }   // domain/model/BodyProfile.kt
interface ProfileRepository { val status: StateFlow<ProfileStatus>; suspend fun refresh(); ... }
```

| Lỗ hổng | Cách sửa | File |
|---|---|---|
| Đăng ký xong nhưng bỏ dở onboarding: lần sau đăng nhập vẫn vào Dashboard với hồ sơ mặc định | `AuthViewModel` chờ `status != LOADING` (nút vẫn quay trong lúc chờ). `MISSING` thì sang Onboarding, còn lại sang Dashboard | `auth/AuthViewModel.kt` |
| Tải lỗi thì lặng lẽ dùng hồ sơ mặc định | Dashboard hiện `VfErrorState` + "Thử lại", gọi `refresh()` | `dashboard/*` |
| Dashboard hiện mục tiêu mặc định vài trăm ms rồi mới đổi | `isLoading = status == LOADING` | `DashboardViewModel.kt` |
| Màn sửa hồ sơ có thể lưu giá trị mặc định đè lên hồ sơ thật | Ở chế độ sửa, form bị ẩn (hiện loading hoặc lỗi + thử lại) cho tới khi `READY`. Khi `READY` thì nạp hồ sơ vừa tải. `Submit` bị chặn | `onboarding/*` |
| Không có nút đăng xuất | Nút "Đăng xuất" trên màn Hồ sơ → `authRepository.logout()` → về màn đăng nhập và xóa sạch back stack | `onboarding/*`, `VisionFitNavHost.kt` |

Bản Mock: `status` luôn là `READY` và `refresh()` không làm gì, nên luồng demo và các test cũ giữ nguyên.

**Test thêm:**
- `ProfileGateTests` (5 test, dùng `FakeProfileRepository`): đăng nhập chờ hồ sơ · `MISSING` thì sang Onboarding · Dashboard loading, lỗi, thử lại · màn sửa hồ sơ không lưu giá trị mặc định · đăng xuất
- `RemoteProfileRepositoryTest` lên 7 test: các trạng thái `MISSING/FAILED`, `refresh()` sau khi lỗi

**Kiểm tra trên giao diện thật với backend thật** (Compose UI test tạm thời, đã xóa sau khi chạy):
1. Đăng ký trên UI → Onboarding → tăng tuổi lên 30 → "Tính mục tiêu" → Goal → Dashboard ✓
2. Hồ sơ → Đăng xuất → về màn đăng nhập, session = null ✓
3. Đăng nhập lại → Dashboard (status `READY`) → Hồ sơ hiện tuổi **30** tải từ server ✓
4. User đăng ký qua API nhưng chưa có hồ sơ → đăng nhập trên UI → được đưa về Onboarding (status `MISSING`) ✓
5. Tắt profile-service → đăng nhập → Dashboard hiện "Chưa tải được hồ sơ" + "Thử lại"; bấm lại khi server vẫn sập thì vẫn hiện lỗi, không crash ✓

Kết quả: `:composeApp:jvmTest` có **62 test xanh**, iOS và Android compile được.

**Còn lại (chấp nhận được):** History, Review và Goal không có màn lỗi riêng. Nếu tải hồ sơ lỗi mà người dùng chuyển tab, các màn đó sẽ tính theo hồ sơ mặc định. Dashboard là màn đầu tiên sau đăng nhập và đã chặn trường hợp này.

### Lưu ý môi trường (máy Mac)

- `visionfit-backend/gradlew` **không có quyền thực thi** (repo được tạo trên Windows), nên phải chạy `sh ./gradlew ...`. Muốn sửa hẳn thì dùng `git update-index --chmod=+x gradlew` rồi commit.
- File untracked `designsystem/components/Responsive.kt` (tham chiếu tới `VfDimens.MaxContentWidth`, một hằng số không còn tồn tại) làm app không compile được. **Đã xóa** theo yêu cầu của user.

---

## 8. Hướng dẫn chạy local

```bash
# 1. Database (lần đầu: cp .env.example .env rồi sửa mật khẩu)
docker compose up -d identity-db profile-db

# 2. Hai service, mỗi service một terminal (trên Mac: sh ./gradlew nếu gradlew chưa có quyền thực thi)
./gradlew :identity-service:bootRun
./gradlew :profile-service:bootRun

# 3. App desktop (trong repo BaoFit)
./gradlew :desktopApp:run
```

Spring Boot **không tự đọc file `.env`**. Khi chạy bằng `bootRun`, service dùng giá trị mặc định trong `application.properties`, và các giá trị này đã khớp với `.env.example`. Nếu bạn đổi `JWT_SECRET` trong `.env` thì phải `export` biến đó cho **cả hai** service, nếu không mọi token sẽ bị profile-service trả 401.
