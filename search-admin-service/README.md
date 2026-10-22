# Search & Admin Service (`search-admin-service`)

- **Người phụ trách (Owner)**: Nguyễn Tấn Quốc (`CE190614`)
- **Port mặc định**: `8086`
- **Kho dữ liệu riêng (DB per service)**: **Elasticsearch 8** (`:9200`) — không dùng SQL Database.
  - `music_tracks`: Lưu bài hát + vector embedding 384 chiều (`dense_vector`, `cosine`) + cờ `needsEmbedding`.
  - `music_albums`: Lưu thông tin album phục vụ tìm kiếm.
  - `music_artists`: Lưu thông tin nghệ sĩ phục vụ tìm kiếm.
  - `search_curations`: Lưu cấu hình ghim kết quả tìm kiếm theo từ khóa (Curation CRUD).

---

## 1. Cách Chạy Service

### Yêu cầu hạ tầng
```bash
docker compose up -d rabbitmq elasticsearch
```

### Biến môi trường (`search-admin-service/.env`)
```env
RABBITMQ_ADDRESSES=amqp://guest:guest@localhost:5672
GATEWAY_SECRET=<secret-dung-chung-voi-gateway>
INTERNAL_TOKEN=<token-noi-bo-giua-cac-service>
ELASTICSEARCH_URL=http://localhost:9200
AI_SERVICE_URL=http://localhost:8000
AI_API_KEY=<api-key-ai-service>
```
*(Khi test độc lập 1 mình trên máy, có thể đặt `EUREKA_ENABLED=false` và để trống `GATEWAY_SECRET=`).*

### Lệnh Build & Test
```bash
# Chạy toàn bộ 16 Unit Tests (JUnit 5 + Mockito)
./mvnw -pl search-admin-service test

# Build đóng gói JAR
./mvnw -pl search-admin-service -am -DskipTests package
```

### Swagger UI
- Truy cập khi service đang chạy: `http://localhost:8086/swagger-ui.html`

---

## 2. Danh Sách Endpoints Chính

| Nhóm | Method & Path | Quyền | Mô tả |
| :--- | :--- | :---: | :--- |
| **Public Search** | `GET /api/search?q=&type=all&page=0&size=20` | Public | Tìm kiếm đa trường (`multi_match` + `fuzziness: AUTO`), ưu tiên bài được ghim (Curation) lên đầu trang 1. |
| **Public Suggest** | `GET /api/search/suggest?q=` | Public | Gợi ý tự động hoàn thành từ khóa (`match_phrase_prefix`). |
| **Internal API** | `POST /internal/search/recommend` | Internal (`X-Internal-Token`) | Tính vector trung bình từ `likedTrackIds`, truy vấn kNN cosine trên ES, loại trừ `excludeTrackIds` & `dislikedTrackIds`. |
| **Admin Curation** | `GET /api/admin/curations` | `ADMIN` | Lấy danh sách toàn bộ cấu hình ghim từ khóa. |
| **Admin Curation** | `GET /api/admin/curations/{id}` | `ADMIN` | Xem chi tiết 1 curation theo ID. |
| **Admin Curation** | `POST /api/admin/curations` | `ADMIN` | Tạo mới curation ghim bài hát cho từ khóa. |
| **Admin Curation** | `PUT /api/admin/curations/{id}` | `ADMIN` | Cập nhật curation theo ID. |
| **Admin Curation** | `DELETE /api/admin/curations/{id}` | `ADMIN` | Xóa curation theo ID. |
| **Admin Reindex** | `POST /api/admin/search/reindex` | `ADMIN` | Chạy đồng bộ lại toàn bộ bài hát từ `content-service` sang ES (bất đồng bộ, trả `202 Accepted`). |
| **Admin Reindex** | `GET /api/admin/search/status` | `ADMIN` | Kiểm tra tiến độ và trạng thái của lần reindex gần nhất. |
| **Admin Dashboard**| `GET /api/admin/dashboard` | `ADMIN` | Tổng hợp thống kê từ `user`, `content`, `engagement`, `analytics` và trạng thái `search`. |
| **Admin System** | `POST /api/admin/system/cleanup-history?days=90` | `ADMIN` | Gọi `analytics-service` dọn lịch sử nghe nhạc cũ hơn `days` ngày. |

---

## 3. Business Rules & Xử Lý Lỗi

1. **Curation Keyword Uniqueness (`CurationServiceImpl.java`)**:
   - Mỗi từ khóa (`keyword`, chuẩn hóa chữ thường `trim().toLowerCase()`) chỉ có tối đa 1 Curation. Tạo mới hoặc cập nhật trùng từ khóa trả về **`409 Conflict`** (`BusinessException`).
   - Danh sách `trackIds` tự động khử trùng lặp và giữ nguyên thứ tự ưu tiên (`LinkedHashSet`).
2. **Single-Execution Reindex (`IndexingServiceImpl.java`)**:
   - Tại một thời điểm chỉ cho phép **1 tiến trình reindex** chạy nền (kiểm soát bằng `AtomicBoolean.compareAndSet(false, true)`). Gọi tiếp khi đang chạy trả về **`409 Conflict`**.
3. **Search Input Validation (`SearchServiceImpl.java`)**:
   - Từ khóa `q` bắt buộc từ 1 đến 100 ký tự; `type` chỉ chấp nhận `all | tracks | albums | artists`. Vi phạm trả về **`400 Bad Request`**.
4. **RBAC Admin Protection (`AdminController.java`)**:
   - Toàn bộ `/api/admin/**` yêu cầu `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`). Không đăng nhập trả `401`, sai role trả `403`.

---

## 4. Sự Kiện RabbitMQ & Circuit Breaker (Resilience4j)

### Events lắng nghe (`ContentSyncListener.java` — Queue: `search-admin.content.events`)
Đồng bộ dữ liệu sang Elasticsearch theo mô hình **Event-Driven CQRS Read-Model** (Idempotent upsert/delete; lỗi sau 3 lần retry sẽ chuyển vào DLQ `search-admin.content.events.dlq`):
- `content.track.created`, `content.track.updated`, `content.track.deleted`
- `content.album.created`, `content.album.updated`, `content.album.deleted`
- `content.artist.created`, `content.artist.updated`, `content.artist.deleted`
- `content.category.updated` (cập nhật tên thể loại bằng Painless script `update_by_query`), `content.category.deleted`

### Circuit Breaker & Fallback
- **`elasticsearch` (`SearchServiceImpl`)**: Khi ES gặp sự cố, tự động fallback sang tìm kiếm bằng `LIKE` trên DB của `content-service` (`source = "database"`).
- **`ai` (`AiClient`)**: Khi `AI-service` tắt/lỗi, vẫn index bài hát vào ES với cờ `needsEmbedding = true`; job `@Scheduled` `embedPending()` tự động quét và embed bù định kỳ.
- **Feign Clients (`AdminClientsFallbacks`)**: Khi các service con (`user`, `content`, `engagement`, `analytics`) ngừng phản hồi, ô thống kê tương ứng trên Admin Dashboard trả `null` để trang quản trị vẫn hoạt động bình thường.
