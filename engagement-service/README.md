# Engagement Service (`engagement-service`)

- **Người phụ trách (Owner)**: Lê Hoàng Tính (`CE190016`)
- **Port mặc định**: `8083`
- **Kho dữ liệu riêng (DB per service)**: **PostgreSQL (Neon)** — Database `music4_engagement`, quản lý bằng Flyway migration (`V1__init_schema.sql`, `V2__notification_details.sql`):
    - `playlists`: Danh sách phát cá nhân của người dùng (`user_id`, `name` unique theo từng user, `is_public`).
    - `playlist_tracks`: Bảng trung gian lưu các bài hát trong playlist (`playlist_id`, `track_id` unique).
    - `favorites`: Bài hát yêu thích của người dùng (`user_id`, `track_id` unique).
    - `follows`: Theo dõi nghệ sĩ (`user_id`, `artist_id` unique).
    - `ratings`: Đánh giá sao bài hát từ `1` đến `5` (`user_id`, `track_id` unique).
    - `comments`: Bình luận bài hát (`track_id`, `user_id`, `content` tối đa 500 ký tự).
    - `notifications`: Thông báo người dùng khi nghệ sĩ đang theo dõi phát hành bài hát/album mới (`user_id`, `artist_id`, `track_id`, `album_id`, `is_read`).
    - `processed_events`: Lưu `event_id` đã xử lý để đảm bảo tính **Idempotent** khi nhận event từ RabbitMQ.

---

## 1. Cách Chạy Service

### Yêu cầu hạ tầng
```bash
docker compose up -d rabbitmq
```

### Lệnh Build & Test
```bash
# Chạy toàn bộ Unit Tests (JUnit 5 + Mockito)
./mvnw -pl engagement-service test

# Build đóng gói JAR
./mvnw -pl engagement-service -am -DskipTests package
```

### Swagger UI & Health Check
- **Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **Actuator Health**: `http://localhost:8083/actuator/health`

---

## 2. Danh Sách Endpoints Chính

| Nhóm | Method & Path | Quyền | Mô tả |
| :--- | :--- | :---: | :--- |
| **Playlists** | `GET /api/playlists` hoặc `/api/playlists/me` | Authenticated | Danh sách toàn bộ playlist của người dùng đang đăng nhập. |
| **Playlists** | `GET /api/playlists/user/{userId}` | Public / Owner | Xem playlist của một user (người khác chỉ thấy playlist `isPublic = true`). |
| **Playlists** | `GET /api/playlists/{id}` & `/{id}/tracks` | Public / Owner | Xem chi tiết playlist kèm danh sách bài hát (nếu private chỉ chủ sở hữu hoặc `ADMIN` xem được). |
| **Playlists** | `POST /api/playlists` | Authenticated | Tạo playlist mới (tối đa 50 playlist/user, không trùng tên trong cùng user). |
| **Playlists** | `PUT /api/playlists/{id}` & `PUT /api/playlists/{id}/image` | Owner / `ADMIN` | Cập nhật thông tin hoặc ảnh bìa playlist. |
| **Playlists** | `DELETE /api/playlists/{id}` | Owner / `ADMIN` | Xóa playlist. |
| **Playlists** | `POST /api/playlists/{id}/tracks` | Owner / `ADMIN` | Thêm bài hát vào playlist (không trùng bài, tối đa 500 bài/playlist). |
| **Playlists** | `DELETE /api/playlists/{id}/tracks/{trackId}` | Owner / `ADMIN` | Xóa bài hát khỏi playlist. |
| **Favorites** | `POST /api/favorites/toggle/{trackId}` | Authenticated | Thả tim / bỏ tim bài hát (toggle). |
| **Favorites** | `GET /api/favorites/me` & `/check/{trackId}` | Authenticated | Lấy danh sách bài hát đã thích và kiểm tra trạng thái thích của 1 bài. |
| **Favorites** | `GET /api/favorites/track/{trackId}/count` | Public | Đếm tổng lượt yêu thích của một bài hát. |
| **Follows** | `POST /api/follows/toggle?artistId={artistId}` | Authenticated | Theo dõi / bỏ theo dõi nghệ sĩ (không được tự follow chính mình). |
| **Follows** | `GET /api/follows/user/{userId}` & `/artist/{artistId}/count` | Public | Danh sách nghệ sĩ mà user đang theo dõi và tổng số người theo dõi của nghệ sĩ. |
| **Follows** | `GET /api/artists/{id}/followers` | Authenticated | Lấy danh sách người theo dõi của nghệ sĩ. |
| **Ratings** | `POST /api/ratings` | Authenticated | Đánh giá bài hát (`1..5` sao, gửi lại sẽ cập nhật điểm cũ - upsert). |
| **Ratings** | `GET /api/ratings/average/{trackId}` | Public | Điểm đánh giá trung bình (làm tròn 1 chữ số thập phân) và tổng lượt đánh giá. |
| **Ratings** | `GET /api/ratings/track/{trackId}/me` & `DELETE /api/ratings/track/{trackId}` | Authenticated | Xem hoặc xóa đánh giá của chính mình cho bài hát. |
| **Comments** | `GET /api/comments/track/{trackId}?page=0&size=20` | Public | Danh sách bình luận của bài hát (phân trang, mới nhất trước). |
| **Comments** | `POST /api/comments` | Authenticated | Viết bình luận mới (`1..500` ký tự, giới hạn chống spam tối đa 5 bình luận/phút). |
| **Comments** | `PUT /api/comments/{id}` | Owner | Chỉnh sửa nội dung bình luận (chỉ tác giả bình luận). |
| **Comments** | `DELETE /api/comments/{id}` | Owner / `ADMIN` | Xóa bình luận (tác giả bình luận hoặc `ADMIN`). |
| **Notifications** | `GET /api/notifications/me` & `/user/{userId}` | Owner / `ADMIN` | Danh sách thông báo của người dùng và số lượng chưa đọc (`/unread-count`). |
| **Notifications** | `GET /api/notifications/subscribe/{userId}` | Owner / `ADMIN` | Kết nối **SSE (Server-Sent Events)** nhận thông báo thời gian thực (`?token=`). |
| **Notifications** | `PUT /api/notifications/{id}/read` & `/read-all` | Owner | Đánh dấu đã đọc 1 thông báo hoặc tất cả thông báo. |
| **Notifications** | `DELETE /api/notifications/{id}` & `/clear-all` | Owner | Xóa 1 thông báo hoặc xóa sạch thông báo của bản thân. |
| **Internal API** | `/internal/users/{userId}/signals`, `/internal/artists/{artistId}/stats`, `/internal/stats` | Internal (`X-Internal-Token`) | API nội bộ cung cấp tín hiệu gợi ý và thống kê tương tác cho `analytics-service`, `content-service` và `search-admin-service`. |

---

## 3. Business Rules & Xử Lý Lỗi

1. **Playlist Ownership, Uniqueness & Limits (`PlaylistServiceImpl.java`)**:
    - Mỗi người dùng chỉ được tạo tối đa **50 playlist** (`MAX_PLAYLISTS_PER_USER = 50` $\rightarrow$ **`409 Conflict`**).
    - Tên playlist không được trùng trong phạm vi cùng một người dùng (`existsByUserIdAndNameIgnoreCase` $\rightarrow$ **`409 Conflict`**).
    - Chỉ chủ sở hữu playlist hoặc `ADMIN` mới được sửa/xóa hoặc xem playlist riêng tư (`isPublic = false` $\rightarrow$ **`403 Forbidden`**).
    - Không được thêm trùng bài hát đã có trong playlist (**`409 Conflict`**), tối đa **500 bài hát/playlist**, và bài hát phải tồn tại bên `content-service` (**`404 Not Found`**).
2. **Follow Self-Protection & Favorite/Rating Upsert (`LibraryServiceImpl.java`)**:
    - Nghệ sĩ không thể tự theo dõi hồ sơ nghệ sĩ của chính mình (`Objects.equals(artist.userId(), userId)` $\rightarrow$ **`409 Conflict`**: *"Không thể tự theo dõi chính mình"*).
    - Mỗi người dùng chỉ thích 1 lần/bài hát (gọi lại sẽ hủy thích) và chỉ có 1 đánh giá (`1..5` sao)/bài hát (gọi lại sẽ cập nhật điểm đánh giá).
3. **Comment Rate-Limit & Ownership (`CommentServiceImpl.java`)**:
    - Nội dung bình luận từ `1..500` ký tự; bài hát phải tồn tại bên `content-service` (**`404 Not Found`**).
    - Chống spam: tối đa **5 bình luận/phút** trên mỗi người dùng (`countByUserIdAndCreatedAtAfter >= 5` $\rightarrow$ **`409 Conflict`**).
    - Chỉ người viết mới được sửa bình luận (**`403 Forbidden`**); chỉ người viết hoặc `ADMIN` mới được xóa bình luận (**`403 Forbidden`**).
4. **Notification Privacy (`NotificationServiceImpl.java`)**:
    - Người dùng chỉ được xem, nhận SSE stream, đánh dấu đã đọc hoặc xóa thông báo của chính mình (**`403 Forbidden`**).

---

## 4. Sự Kiện RabbitMQ & Circuit Breaker (Resilience4j)

### Topology & Events lắng nghe (`RabbitTopologyConfig.java` & `EngagementEventListener.java`)
- **Exchange**: `music4.events` (Topic Exchange, durable) & Dead-Letter Exchange `music4.dlx` (Direct Exchange).
- **Queue**: `engagement.events` kèm Dead-Letter Queue `engagement.events.dlq` (tự động đẩy vào DLQ sau 3 lần retry thất bại với backoff `2s -> 4s -> 8s`).
- **Routing keys lắng nghe**:
    - `content.track.created`: Tìm toàn bộ người theo dõi của các nghệ sĩ trong bài hát, lưu thông báo mới và đẩy realtime qua **SSE** (`NEW_TRACK`).
    - `content.album.created`: Tìm toàn bộ người theo dõi của nghệ sĩ, lưu thông báo mới và đẩy realtime qua **SSE** (`NEW_ALBUM`).
    - `content.track.deleted`: Tự động dọn dẹp các bản ghi `playlist_tracks`, `favorites`, `ratings`, `comments`, `notifications` của bài hát đã xóa.
    - `content.artist.deleted`: Tự động dọn dẹp `follows` và `notifications` liên quan đến nghệ sĩ đã xóa.
    - `user.deleted`: Tự động xóa toàn bộ dữ liệu tương tác (`playlists`, `favorites`, `follows`, `ratings`, `comments`, `notifications`) của tài khoản bị xóa.
- **Idempotency**: Kiểm tra `processedEventRepository.existsById(event.eventId())` trước khi xử lý để chống tạo trùng thông báo khi message bị gửi lại.

### Circuit Breaker & Fallback (OpenFeign + Resilience4j)
- **`content-service` (`ContentClientFallbackFactory`)**:
    - Khi lấy danh sách bài hát/nghệ sĩ (`getTracks`, `getArtists`) mà `content-service` gặp sự cố: trả về danh sách rỗng để giao diện vẫn mở được playlist/danh sách yêu thích (các bài thiếu metadata được đánh dấu `available = false`).
    - Khi kiểm tra nghệ sĩ tồn tại (`getArtist`): nếu `404` trả về **`404 Not Found`**, nếu lỗi kết nối/timeout trả về **`503 Service Unavailable`** (*"Không kiểm tra được nghệ sĩ, vui lòng thử lại sau"*).
- **`user-service` (`UserClientFallbackFactory`)**:
    - Khi `user-service` ngừng hoạt động, API danh sách bình luận bài hát vẫn trả về bình thường với thông tin người dùng mặc định thay vì làm lỗi toàn bộ trang nghe nhạc.
