# Content Service (`content-service`)

- **Người phụ trách (Owner)**: Nguyễn Việt Đan Quỳnh (`CE191634`)
- **Port mặc định**: `8082`
- **Kho dữ liệu riêng (DB per service)**: **PostgreSQL (Neon)** — Database `music4_content` (`neondb`), quản lý bằng Flyway migration (`V1__init_schema.sql`, `V2__seed_demo_data.sql`):
    - `artists`: Hồ sơ nghệ sĩ (`user_id` tham chiếu logic sang `user-service`, không dùng Foreign Key chéo DB; hỗ trợ xóa mềm `is_deleted`).
    - `categories`: Danh mục thể loại nhạc (`name` unique).
    - `albums`: Thông tin album nhạc thuộc về nghệ sĩ (`artist_id`).
    - `tracks`: Thông tin bài hát (`duration`, `file_path`, `preview_path`, `view_count`, `album_id`).
    - `track_artists`: Bảng trung gian bài hát – nghệ sĩ với vai trò `MAIN` (nghệ sĩ chính) và `FEAT` (hợp tác).
    - `track_categories`: Bảng trung gian bài hát – thể loại.
    - `processed_events`: Lưu `event_id` đã xử lý để đảm bảo tính **Idempotent** khi nhận event từ RabbitMQ.

---

## 1. Cách Chạy Service

### Yêu cầu hạ tầng
```bash
docker compose up -d rabbitmq
```

### Biến môi trường (`content-service/.env`)
```env
DB_URL=jdbc:postgresql://<host-neon>/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=<mat-khau-neon>
EUREKA_ENABLED=false
GATEWAY_SECRET=<secret-dung-chung-voi-gateway>
INTERNAL_TOKEN=<token-noi-bo-giua-cac-service>
```
*(Khi test độc lập 1 mình trên máy, có thể đặt `EUREKA_ENABLED=false` và để trống `GATEWAY_SECRET=`).*

### Lệnh Build & Test
```bash
# Chạy toàn bộ Unit Tests (JUnit 5 + Mockito)
./mvnw -pl content-service test

# Build đóng gói JAR
./mvnw -pl content-service -am -DskipTests package
```

### Swagger UI & Health Check
- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **Actuator Health**: `http://localhost:8082/actuator/health`

---

## 2. Danh Sách Endpoints Chính

| Nhóm | Method & Path | Quyền | Mô tả |
| :--- | :--- | :---: | :--- |
| **Tracks** | `GET /api/tracks?page=0&size=20&sort=id` | Public | Danh sách bài hát phân trang và sắp xếp (`id`, `name`, `viewCount`, `uploadDate`). |
| **Tracks** | `GET /api/tracks/{id}` | Public | Xem chi tiết bài hát kèm nghệ sĩ, album, thể loại. |
| **Tracks** | `GET /api/tracks/album/{albumId}` | Public | Lấy danh sách bài hát thuộc một album. |
| **Tracks** | `GET /api/tracks/artist/{artistId}` | Public | Lấy danh sách bài hát của một nghệ sĩ. |
| **Tracks** | `GET /api/tracks/trending?limit=10` | Public | Top bài hát thịnh hành theo lượt nghe (`viewCount`). |
| **Tracks** | `GET /api/tracks/stream/{id}` | Public / User | Redirect (`302 Found`) sang `/api/media/stream` để phát nhạc đầy đủ (kèm `?token=`). |
| **Tracks** | `GET /api/tracks/stream/preview/{id}` | Public | Redirect (`302 Found`) sang `/api/media/stream/preview` để nghe thử công khai. |
| **Tracks** | `POST /api/tracks` (`multipart/form-data`) | `ARTIST`, `ADMIN` | Upload bài hát mới kèm `audioFile` và `coverImage` qua `media-service`. |
| **Tracks** | `POST /api/tracks/upload-temp` | `ARTIST`, `ADMIN` | Upload trước file lẻ (`type=images\|tracks`) để lấy `objectKey` và `duration`. |
| **Tracks** | `POST /api/tracks/bulk-json` | `ARTIST`, `ADMIN` | Lưu hàng loạt bài hát vào album từ các `objectKey` đã upload. |
| **Tracks** | `PUT /api/tracks/{id}` | `ARTIST`, `ADMIN` | Cập nhật thông tin bài hát (chỉ nghệ sĩ chính của bài hoặc `ADMIN`). |
| **Tracks** | `DELETE /api/tracks/{id}` | `ARTIST`, `ADMIN` | Xóa bài hát và bắn event dọn dẹp dữ liệu liên quan. |
| **Albums** | `GET /api/albums` & `GET /api/albums/{id}` | Public | Danh sách album phân trang và chi tiết album kèm các bài hát. |
| **Albums** | `GET /api/albums/artist/{artistId}` | Public | Lấy danh sách album của một nghệ sĩ. |
| **Albums** | `POST /api/albums` (JSON / Multipart) | `ARTIST`, `ADMIN` | Tạo album mới (hỗ trợ cả JSON và `multipart/form-data` kèm ảnh bìa). |
| **Albums** | `PUT /api/albums/{id}` | `ARTIST`, `ADMIN` | Cập nhật tên/ảnh bìa album (chỉ chủ album hoặc `ADMIN`). |
| **Albums** | `DELETE /api/albums/{id}` | `ARTIST`, `ADMIN` | Xóa album (khi album không còn bài hát). |
| **Artists** | `GET /api/artists`, `/all`, `/search`, `/{id}` | Public | Danh sách nghệ sĩ phân trang, tìm kiếm theo từ khóa và xem chi tiết hồ sơ. |
| **Artists** | `GET /api/artists/me` | `ARTIST` | Lấy hồ sơ nghệ sĩ của tài khoản đang đăng nhập. |
| **Artists** | `PATCH /api/artists/{id}` | `ARTIST`, `ADMIN` | Cập nhật hồ sơ nghệ sĩ (chỉ chính nghệ sĩ đó hoặc `ADMIN`). |
| **Artists** | `DELETE /api/artists/{id}` | `ADMIN` | Xóa mềm (`is_deleted = true`) nghệ sĩ khi không còn bài hát. |
| **Categories** | `GET /api/categories` & `GET /api/categories/{id}` | Public | Lấy toàn bộ thể loại nhạc và chi tiết bài hát theo thể loại. |
| **Categories** | `POST / PUT / DELETE /api/categories/**` | `ADMIN` | Tạo mới, cập nhật và xóa thể loại nhạc. |
| **Frontend Compat** | `GET /api/tracks/top5-views`, `/tracks/random`, `/artists/top3`, `/categories/stats` | Public | Các endpoint thống kê & khám phá tương thích trực tiếp với giao diện Frontend. |
| **Internal API** | `/internal/tracks/**`, `/internal/artists/**`, `/internal/stats` | Internal (`X-Internal-Token`) | API nội bộ cung cấp batch tracks/artists, export reindex, fallback search và tạo hồ sơ artist khi đăng ký. |

---

## 3. Business Rules & Xử Lý Lỗi

1. **Category Uniqueness & Deletion Protection (`CategoryServiceImpl.java`)**:
    - Tên thể loại không được trùng (không phân biệt hoa/thường `existsByNameIgnoreCase`). Tạo hoặc sửa trùng tên trả về **`409 Conflict`** (`BusinessException`).
    - Không được xóa thể loại đang được gán cho bất kỳ bài hát nào (`countByCategoryId > 0` $\rightarrow$ **`409 Conflict`**).
2. **Album Ownership, Name Uniqueness & Non-Empty Check (`AlbumServiceImpl.java`)**:
    - Trong phạm vi cùng một nghệ sĩ, tên album không được trùng (`existsByArtistIdAndNameIgnoreCase` $\rightarrow$ **`409 Conflict`**).
    - Chỉ nghệ sĩ sở hữu album hoặc `ADMIN` mới được phép chỉnh sửa/xóa album. Nghệ sĩ khác thao tác trả về **`403 Forbidden`** (`ForbiddenException`).
    - Không được xóa album khi bên trong vẫn còn bài hát (`countByAlbumId > 0` $\rightarrow$ **`409 Conflict`**).
3. **Artist Profile Uniqueness & Soft-Delete Protection (`ArtistServiceImpl.java`)**:
    - Mỗi tài khoản người dùng (`userId`) chỉ được tạo tối đa 1 hồ sơ nghệ sĩ (`existsByUserId` $\rightarrow$ **`409 Conflict`**).
    - Chỉ chính nghệ sĩ đó hoặc `ADMIN` mới được sửa hồ sơ (`SecurityUtils.requireOwnerOrAdmin` $\rightarrow$ **`403 Forbidden`**).
    - Không được xóa nghệ sĩ đang còn bài hát (`countByArtistId > 0` $\rightarrow$ **`409 Conflict`**). Khi xóa sử dụng cơ chế **xóa mềm** (`isDeleted = true`) để không làm gãy tham chiếu ID ở các service khác.
4. **Track Ownership, Album Consistency & Duplicate Title Rule (`TrackServiceImpl.java`)**:
    - Bài hát bắt buộc phải có file âm thanh (`audioFile`), ảnh bìa (`coverImage`) và ít nhất 1 nghệ sĩ chính (`MAIN`).
    - Nếu bài hát được gán vào một album, album đó **bắt buộc phải thuộc về chính nghệ sĩ chính** của bài hát (**`409 Conflict`**).
    - Tên bài hát không được trùng trong cùng một album (`existsByAlbumIdAndNameIgnoreCase` $\rightarrow$ **`409 Conflict`**), kể cả khi upload lẻ hay upload hàng loạt (`saveBulk`).
    - Chỉ nghệ sĩ chính (`MAIN`) của bài hát hoặc `ADMIN` mới được quyền cập nhật hoặc xóa bài hát (**`403 Forbidden`**).

---

## 4. Sự Kiện RabbitMQ & Circuit Breaker (Resilience4j)

### Events phát đi (`EventPublisher.java` — Exchange: `music4.events`)
Khi dữ liệu danh mục thay đổi, `content-service` phát sự kiện để `search-admin-service` đồng bộ sang Elasticsearch, `engagement-service` gửi thông báo cho người theo dõi và dọn dẹp playlist/favorite, `media-service` thu hồi file trên S3:
- **Track**: `content.track.created`, `content.track.updated`, `content.track.deleted`
- **Album**: `content.album.created`, `content.album.updated`, `content.album.deleted`
- **Artist**: `content.artist.created`, `content.artist.updated`, `content.artist.deleted`
- **Category**: `content.category.updated`, `content.category.deleted`

### Events lắng nghe (`TrackPlayedListener.java` — Queue: `content.analytics.track-played`)
- Lắng nghe sự kiện **`analytics.track.played`** từ `analytics-service` để tăng bộ đếm lượt nghe (`view_count`) cho bài hát.
- Xử lý **Idempotent** thông qua bảng `processed_events`: nếu `eventId` đã tồn tại thì bỏ qua, đảm bảo việc nhận lại message (retry) không làm cộng trùng lượt nghe. Lỗi sau 3 lần retry sẽ đẩy vào DLQ (`content.analytics.track-played.dlq`).

### Circuit Breaker & Fallback (OpenFeign + Resilience4j)
- **`media-service` (`MediaClientFallbackFactory`)**:
    - Nếu lỗi `4xx` từ `media-service` (sai định dạng file, vượt dung lượng): giữ nguyên ngữ nghĩa và trả về **`400 Bad Request`** (`BadRequestException`).
    - Nếu `media-service` ngừng hoạt động hoặc quá tải (`5xx` / timeout): kích hoạt fallback trả về **`503 Service Unavailable`** (`ServiceUnavailableException`: *"Dịch vụ lưu trữ file đang bận, vui lòng thử lại sau"*).
- **`engagement-service` (`EngagementClientFallbackFactory`)**:
    - Khi lấy thống kê Top 3 nghệ sĩ (`GET /api/artists/top3`), nếu `engagement-service` gặp sự cố, tự động fallback trả về danh sách rỗng để API vẫn hoạt động bình thường với số lượng `followers = 0` và `favorites = 0`.