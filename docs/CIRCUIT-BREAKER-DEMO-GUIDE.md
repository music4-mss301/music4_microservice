# HƯỚNG DẪN DEMO & VẤN ĐÁP CIRCUIT BREAKER (RESILIENCE4J)
**Người phụ trách chính**: Nguyễn Tấn Quốc (MSSV: CE190614)  
**Dự án**: Music4 Microservices (MSS301)  
**Tiêu chí Barem**: Circuit Breaker (5% điểm chính)

---

## 1. TỔNG QUAN KIẾN TRÚC CIRCUIT BREAKER TRONG DỰ ÁN

Trong kiến trúc Microservices, lỗi dây chuyền (**Cascading Failure**) xảy ra khi một service gặp sự cố hoặc phản hồi chậm, khiến các service gọi nó bị cạn kiệt tài nguyên (thread pool, connection pool) và sập theo.

Dự án áp dụng mẫu thiết kế **Circuit Breaker Pattern** với thư viện **Resilience4j** tích hợp cùng **Spring Cloud OpenFeign** và **Spring Cloud Gateway**.

### Cơ chế hoạt động của Circuit Breaker State Machine:
1. **CLOSED (Đóng - Bình thường)**: Mọi request được chuyển tiếp đến Callee bình thường. Hệ thống giám sát tỷ lệ lỗi trong một cửa sổ trượt (Sliding Window: 10 calls).
2. **OPEN (Mở - Ngắt mạch)**: Khi tỷ lệ lỗi vượt ngưỡng (**failure-rate-threshold: 50%** với tối thiểu 5 cuộc gọi), ngắt mạch lập tức được kích hoạt. Các request tiếp theo sẽ KHÔNG gửi đến Callee nữa mà ngay lập tức thực thi hàm **Fallback**, bảo vệ tài nguyên hệ thống và trả phản hồi nhanh cho client.
3. **HALF-OPEN (Nửa mở - Thử nghiệm)**: Sau thời gian chờ (**wait-duration-in-open-state: 10s**), ngắt mạch cho phép một số lượng cuộc gọi thăm dò (**permitted-number-of-calls-in-half-open-state: 3**) đi qua:
   - Nếu các cuộc gọi thăm dò thành công: Chuyển về **CLOSED**.
   - Nếu vẫn lỗi: Quay lại trạng thái **OPEN** thêm 1 chu kỳ nữa.

---

## 2. MA TRẬN CIRCUIT BREAKER & FALLBACK TRONG TOÀN HỆ THỐNG

| Caller (Service gọi) | Callee (Service đích) | Cơ chế gọi | Hành vi khi Callee gặp sự cố (Fallback) | Vị trí code xử lý |
| :--- | :--- | :--- | :--- | :--- |
| **`analytics-service`** | **`search-admin-service`** | OpenFeign (`recommend`) | Trả về danh sách rỗng ➔ Service tự động chuyển sang lấy bài hát **Trending 7 ngày** | `SearchClientFallbackFactory.java`<br/>`RecommendationServiceImpl.java:61-68` |
| **`search-admin-service`** | **Elasticsearch (`:9200`)** | RestClient / CB Factory | Tự động chuyển hướng tìm kiếm bằng `LIKE` trên Database của `content-service` (`source = "database"`) | `SearchServiceImpl.java:48-52` |
| **`search-admin-service`** | **`AI-service` (`:8000`)** | RestClient / CB Factory | Tạm thời index không vector, đánh dấu `needsEmbedding = true` để embed bù sau | `AiClient.java:37-48`<br/>`IndexingServiceImpl.java:130` |
| **`api-gateway`** | Mọi Service con | Gateway Route Filter | Trả về `HTTP 503 Service Unavailable` định dạng JSON chuẩn, không làm treo kết nối | `FallbackController.java`<br/>`api-gateway/application.yml` |
| **`engagement-service`**| `content-service` | OpenFeign (batch tracks) | Trả về `trackId` kèm cờ `available = false` | `ContentClientFallbackFactory.java` |
| **`engagement-service`**| `user-service` | OpenFeign (batch users) | Trả về tên hiển thị mặc định "Người dùng" | `UserClientFallbackFactory.java` |
| **`search-admin-service`**| 4 Service còn lại | OpenFeign (`stats`) | Trả về `null` cho ô thống kê bị lỗi (Partial Degradation), Admin Dashboard vẫn hoạt động | `AdminClientsFallbacks.java` |

---

## 3. BA KỊCH BẢN DEMO TRỰC TIẾP TRƯỚC GIẢNG VIÊN (QUỐC TRÌNH DIỄN)

### KỊCH BẢN 1: Tắt `search-admin-service` ➔ Gợi ý nhạc tự động chuyển sang Trending (Kịch bản Barem)
> **Mục tiêu**: Chứng minh hệ thống không bị crash khi service AI/Search của Quốc bị sập, người dùng vẫn nhận được nhạc hay để nghe.

1. **Bước 1 (Trạng thái bình thường)**:
   - Đảm bảo `search-admin-service` và `analytics-service` đang chạy.
   - Gọi API lấy gợi ý bài hát:
     ```bash
     curl -i -X GET "http://localhost:8080/api/recommendations" -H "Authorization: Bearer <token>"
     ```
   - *Kết quả*: Nhận danh sách gợi ý bài hát dựa trên sở thích kNN Vector.
2. **Bước 2 (Gây sự cố - Tắt service)**:
   - Dừng `search-admin-service` (bấm nút Stop đỏ trong IntelliJ hoặc tắt terminal chạy port 8086).
3. **Bước 3 (Kiểm tra phản ứng của hệ thống)**:
   - Gọi lại API lấy gợi ý:
     ```bash
     curl -i -X GET "http://localhost:8080/api/recommendations" -H "Authorization: Bearer <token>"
     ```
   - *Kết quả quan sát*:
     - Request vẫn trả về **`200 OK`** thành công!
     - Dữ liệu trả về là danh sách bài hát **Trending** (bài nghe nhiều nhất trong 7 ngày).
     - Xem log của `analytics-service`: Xuất hiện dòng log `WARN: search-admin-service lỗi, dùng trending: ...`.
     - Người dùng trên giao diện Frontend vẫn nghe nhạc bình thường, không hề thấy lỗi 500.

---

### KỊCH BẢN 2: Tắt Elasticsearch ➔ Search tự động chuyển sang DB (`content-service`)
> **Mục tiêu**: Chứng minh tính kiên cường nội tại của `search-admin-service`.

1. **Bước 1 (Trạng thái bình thường)**:
   - Elasticsearch đang chạy (`docker ps`).
   - Gọi tìm kiếm từ khóa:
     ```bash
     curl -s "http://localhost:8086/api/search?q=pop" -H "X-Gateway-Secret: 54d68086ce267ea24ba6d2fe82fe0645fb74d8c0aa1bdf08"
     ```
   - *Kết quả*: JSON trả về có trường `"source": "elasticsearch"`.
2. **Bước 2 (Gây sự cố Elasticsearch)**:
   - Dừng container Elasticsearch:
     ```powershell
     docker stop music4-elasticsearch
     ```
3. **Bước 3 (Kiểm tra Fallback)**:
   - Gọi lại lệnh tìm kiếm:
     ```bash
     curl -s "http://localhost:8086/api/search?q=pop" -H "X-Gateway-Secret: 54d68086ce267ea24ba6d2fe82fe0645fb74d8c0aa1bdf08"
     ```
   - *Kết quả*: 
     - Vẫn trả về **`200 OK`**!
     - Trường `"source"` đổi thành `"database"` (tìm bằng LIKE qua database của `content-service`).
     - Log của `search-admin-service` hiển thị: `WARN: ES lỗi, fallback tìm trong DB: ...`.
4. **Bước 4 (Phục hồi)**:
   - Bật lại Elasticsearch: `docker start music4-elasticsearch`.
   - Sau vài giây, gọi lại tìm kiếm sẽ tự động phục hồi về `"source": "elasticsearch"`.

---

### KỊCH BẢN 3: Tắt Service ➔ Gateway kích hoạt Fallback 503 JSON chuẩn
> **Mục tiêu**: Chứng minh Gateway không bị treo connection (Hanging Request) khi service backend chết.

1. Tắt `search-admin-service`.
2. Gọi request qua Gateway:
   ```bash
   curl -i -X GET "http://localhost:8080/api/search?q=pop"
   ```
3. *Kết quả*: Thay vì chờ timeout 30s-60s hoặc lỗi màn hình trắng, Gateway lập tức trả về **`HTTP 503 Service Unavailable`**:
   ```json
   {
     "status": 503,
     "error": "Service Unavailable",
     "message": "search-admin-service đang tạm thời không phản hồi, vui lòng thử lại sau",
     "service": "search-admin-service"
   }
   ```

---

## 4. QUAN SÁT CIRCUIT BREAKER QUA ACTUATOR & MONITORING

Khi demo, Quốc có thể mở sẵn các URL sau trên trình duyệt để giảng viên thấy số liệu thời gian thực:

1. **Trạng thái Health & Circuit Breakers**:
   - `http://localhost:8086/actuator/health` ➔ Xem mục `circuitBreakers`.
   - `http://localhost:8086/actuator/circuitbreakers` ➔ Xem danh sách các Circuit Breaker và trạng thái (`CLOSED` hay `OPEN`).
2. **Số liệu Metrics**:
   - `http://localhost:8086/actuator/metrics/resilience4j.circuitbreaker.state` ➔ Đo trạng thái ngắt mạch để Prometheus scrape vào Grafana.
3. **Grafana Dashboard (Nếu Kiên đã bật container monitoring)**:
   - `http://localhost:3000` ➔ Mở Dashboard "Music4 - Circuit Breakers" quan sát biểu đồ State chuyển từ xanh (Closed) sang đỏ (Open).

---

## 5. BỘ CÂU HỎI VẤN ĐÁP GIẢNG VIÊN HAY HỎI (KÈM CÂU TRẢ LỜI CHO QUỐC)

### Câu 1: Circuit Breaker là gì? Tại sao hệ thống Microservices bắt buộc phải có?
**Trả lời**:
> "Dạ thưa Thầy/Cô, trong kiến trúc Microservices, các service giao tiếp qua mạng nên luôn có rủi ro độ trễ cao hoặc mất kết nối. Nếu không có Circuit Breaker, khi một service downstream phản hồi chậm hoặc chết, các thread của service upstream sẽ bị giữ lại để chờ đợi, dẫn tới cạn kiệt thread pool và gây sập toàn bộ hệ thống (lỗi dây chuyền - Cascading Failure).  
> Circuit Breaker hoạt động như một cầu dao điện: khi phát hiện lỗi vượt ngưỡng quy định, nó sẽ ngắt mạch (chuyển sang trạng thái OPEN) và kích hoạt hàm Fallback ngay lập tức mà không gửi request qua mạng nữa, giúp hệ thống phản hồi nhanh và giải phóng tài nguyên."

### Câu 2: Em cấu hình các tham số của Resilience4j như thế nào và ý nghĩa của chúng?
**Trả lời**:
> "Dạ, em cấu hình Resilience4j tập trung trong `config-repo/application.yml` và ghi đè riêng cho từng service với các thông số:
> - `sliding-window-size: 10`: Cửa sổ trượt ghi nhận 10 cuộc gọi gần nhất.
> - `minimum-number-of-calls: 5`: Cần tối thiểu 5 cuộc gọi trong cửa sổ trước khi bắt đầu tính toán tỷ lệ lỗi (tránh ngắt mạch oan lúc mới khởi động).
> - `failure-rate-threshold: 50`: Nếu tỷ lệ lỗi đạt từ 50% trở lên, mạch sẽ nhảy sang trạng thái OPEN.
> - `wait-duration-in-open-state: 10s`: Khi mạch OPEN, nó giữ nguyên trong 10 giây trước khi chuyển sang HALF_OPEN để thăm dò.
> - `permitted-number-of-calls-in-half-open-state: 3`: Cho phép 3 cuộc gọi thử nghiệm khi HALF_OPEN. Nếu thành công sẽ đóng mạch trở lại."

### Câu 3: Khi nào dùng Circuit Breaker Fallback, khi nào nên để trả lỗi 503?
**Trả lời**:
> "Dạ, điều này phụ thuộc vào tính chất nghiệp vụ của từng thao tác (Read vs Write):
> - **Đối với thao tác Đọc (Read / Query)**: Luôn ưu tiên **Graceful Degradation** (suy giảm chức năng nhẹ nhàng). Ví dụ: gợi ý nhạc chết thì chuyển sang Trending; Elasticsearch chết thì tìm tạm bằng LIKE trên DB của content; ô số liệu dashboard lỗi thì trả null. Người dùng vẫn xem được nội dung.
> - **Đối với thao tác Ghi (Write / Command)**: Ví dụ upload nhạc lên S3, đăng ký tài khoản, hoặc reindex dữ liệu. Lúc này không thể làm giả dữ liệu hay đoán mò được, nên Circuit Breaker phải trả về **HTTP 503** rõ ràng để client hoặc user biết thao tác chưa thành công và thử lại sau."
