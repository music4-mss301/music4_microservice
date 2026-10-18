# Patch src: Music4 monolith -> microservices

## Chạy local nhanh
1. SQL Server bằng Docker: `docker compose -f docker-compose.db.yml up -d` (tự tạo 5 DB `Music4_User`, `Music4_Content`,
   `Music4_Engagement`, `Music4_Analytics`, `Music4_Media` và 5 login riêng qua `deploy/sqlserver/init.sql`).
2. RabbitMQ + Elasticsearch 8 (docker-compose sẵn có).
3. Thứ tự: `config-server` → `eureka-server` → 6 service → `api-gateway` → AI-service.
4. Flyway tự tạo bảng + seed: `admin/Admin123`, `artist/Artist123`, `listener/Listener123` (đã BCrypt).
5. Build: `mvn -q -DskipTests package` ở thư mục gốc. **Code chưa được build bằng Maven** (môi trường sinh code
   không truy cập được Maven Central), mới kiểm tra cú pháp toàn bộ file và tham chiếu class nội bộ.

## Thay đổi so với zip cũ
- Giữ SQL Server (giảng viên yêu cầu chạy bằng Docker): migration T-SQL viết lại (V1 schema + V2 seed), mọi chuỗi là NVARCHAR
  (`hibernate.use_nationalized_character_data=true`) để lưu tiếng Việt. Production dùng Azure SQL Database free (cùng engine).
- Thêm module `config-server` + thư mục `config-repo/`.
- Mọi service: Spring Security (header từ gateway), Actuator, AMQP, Resilience4j, Config client.
- Xóa `*ApplicationTests.contextLoads` (cần DB thật nên sẽ fail trên CI). Cần viết lại unit test.
- `Artist` (content) bỏ email/password: mọi tài khoản nằm ở user-service, `users.artist_id` trỏ hồ sơ nghệ sĩ.
- Comment chỉ còn `user_id` (artist cũng là user). Playlist thêm `is_public`.

## Frontend cần sửa
- `artistId` KHÁC `userId`: dùng `artistId` trong response login/JWT cho trang nghệ sĩ, album, thống kê.
- Nghe nhạc đầy đủ: `${API}/tracks/stream/{id}?token=${accessToken}` (thẻ audio không gửi header).
- Ảnh/nhạc: đọc qua `/api/media/file?key=...` hoặc `/api/media/stream/preview?key=...`.
- Follow: `POST /follows/toggle?artistId=` (userId lấy từ token). Đếm follower: `/follows/artist/{id}/count`.
- Rating trung bình trả object `{trackId, average, count}`; tạo rating không gửi `userId`.
- Bỏ các URL hardcode `http://localhost:8080` trong NotificationBell, axios refresh.
