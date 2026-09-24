# 📋 TEAM_RULES.md — Quy định Làm việc Nhóm & Chuẩn Môn học PRM393
## 🎵 Music Streaming App — PRM393 (Flutter & Spring Boot Microservices)

> **Phiên bản**: 3.0.0 (Cập nhật sau Team Meeting 24/09/2026)  
> **Áp dụng cho**: 5 thành viên (T1, T2, T3, T4, T5)  
> **Mục tiêu**: Tối ưu điểm **C6 (1.5đ Git/Process/AI Log)**, **C8 (0.5đ Ownership)**, **C10 (2.5đ Defense)** và **+0.5đ Bonus**.

---

## 🌿 PHẦN 1: QUY TẮC GIT & BRANCHING STRATEGY

### 1.1 Mô hình Phân nhánh (Git Branching)
Mỗi thành viên làm việc độc lập trên branch của mình, **TUYỆT ĐỐI KHÔNG commit trực tiếp vào `main` hoặc `develop`**.

```text
main                              ← Branch nộp bài / Release APK (trigger build APK khi merge)
│
develop                           ← Branch tích hợp chung (PR vào đây được CI/CD test tự động)
│
├── feature/CE190036-stream-audio      ← T1: luồng stream nhạc
├── feature/CE190036-album-crud        ← T1: CRUD album (Artist Studio)
├── feature/CE190737-login             ← T2: đăng nhập
├── feature/CE190737-oauth-google      ← T2: Google OAuth2
├── feature/CE190284-home-feed         ← T3: trang chủ
├── feature/CE190284-ai-recommendation ← T3: gợi ý AI
├── feature/CE191634-playlist-crud     ← T4: quản lý playlist
├── feature/CE191634-album-detail      ← T4: chi tiết album (Listener view)
├── feature/CE190614-search            ← T5: tìm kiếm Elasticsearch
├── feature/CE190614-voice-search      ← T5 BONUS: Voice Search
├── bugfix/CE190737-fix-token-refresh  ← Sửa lỗi
└── release/v1.0.0                     ← Chuẩn bị nộp bài
```

**Cú pháp đặt tên nhánh BẮT BUỘC:**
- Feature mới: `feature/[MSSV]-[usecase]` (VD: `feature/CE190614-voice-search`, `feature/CE190036-album-crud`, `feature/CE190737-login`)
- Sửa lỗi: `bugfix/[MSSV]-[mo-ta-loi]` (VD: `bugfix/CE190737-fix-token-refresh`)
- Chore/CI: `chore/[MSSV]-[mo-ta]` (VD: `chore/CE190614-setup-ci-cd`)

> ⚠️ **Lưu ý**: `[usecase]` phải là tên use case ngắn gọn, dùng **kebab-case**, **KHÔNG dùng `week-N`**. Mỗi nhiệm vụ (use case) = 1 nhánh riêng, mở PR khi xong.

---

### 1.2 Quy chuẩn Commit Message (BẮT BUỘC có MSSV để lấy điểm C8 & C6)

> ⚠️ **Quy định môn học**: Commit message không có Mã Sinh Viên sẽ **không thể chứng minh ai là tác giả đoạn code đó** khi bảo vệ trước hội đồng.

**Format chuẩn:**
```text
[MSSV] <type>(<scope>): <Mô tả ngắn gọn bằng tiếng Anh hoặc tiếng Việt rõ nghĩa>
```

**Các `type` chuẩn Conventional Commits:**
- `feat`: Thêm tính năng mới (UI, Logic, API)
- `fix`: Sửa lỗi (bug fix)
- `refactor`: Tái cấu trúc code (không đổi logic bên ngoài)
- `test`: Viết thêm Unit Test hoặc Widget Test
- `docs`: Sửa tài liệu, README, AI Log
- `chore`: Cấu hình build, cài thêm package pubspec

**Ví dụ Hợp Lệ (Ăn trọn điểm C6, C8):**
- `[CE190036] feat(player): implement background audio service with LockCachingAudioSource`
- `[CE190036] feat(album): add AlbumManagementScreen with image picker and track assign`
- `[CE190737] feat(auth): add auto-refresh token interceptor for Dio`
- `[CE190284] feat(home): integrate AI recommendations API and render horizontal list`
- `[CE191634] feat(playlist): add create and delete playlist with optimistic UI`
- `[CE190614] feat(search): implement Elasticsearch search with 300ms debounce`
- `[CE190614] feat(search): add voice search with speech_to_text and RECORD_AUDIO permission`
- `[CE190036] test(player): add unit test for PlayerNotifier state transitions`

**Ví dụ BỊ TRỪ ĐIỂM (Cấm tuyệt đối):**
- ❌ `update`
- ❌ `fix bug`
- ❌ `commit code`
- ❌ `WIP`
- ❌ `feat: update UI` (Thiếu MSSV)

**Tần suất tối thiểu**: Mỗi người phải có **ít nhất 2 - 3 commits có ý nghĩa / tuần** (rải đều các ngày trong tuần, không được dồn vào ngày nộp bài).

---

## 👥 PHẦN 2: CODE OWNERSHIP & MA TRẬN PHÂN CÔNG (5 THÀNH VIÊN)

| Thành viên | MSSV & Họ tên | Domain phụ trách | Màn hình sở hữu (Vertical Slice) | Bonus Target (+0.5đ) |
|---|---|---|---|---|
| **T1** | `CE190036` — Nguyễn Trung Kiên | Media & Streaming | `MusicPlayerScreen`, `UploadTrackScreen`, `AlbumManagementScreen` *(Artist Studio)* | **Offline-first**: Cache nhạc cục bộ với `LockCachingAudioSource` (`just_audio`) an toàn cho Byte-range stream |
| **T2** | `CE190737` — Lê Minh Nhựt | Auth & User Profile | `LoginScreen`, `RegisterScreen`, `UserProfileScreen` | **Deep Link**: Google OAuth2 callback (`app_links`) |
| **T3** | `CE190284` — Nguyễn Hữu Tài | Discovery & AI | `HomeScreen`, `HistoryScreen`, `ArtistProfileScreen`, `FavoritesScreen` | **Real-time Data**: Tự động refresh gợi ý nhạc sau khi nghe |
| **T4** | `CE191634` — Nguyễn Việt Đan Quỳnh | Content & Engagement | `AlbumDetailScreen` *(Listener view)*, `TrackDetailScreen`, `PlaylistScreen`, `PlaylistDetailScreen` | **Device / Share**: Tạo shareable link nhạc `share_plus` |
| **T5 (Leader)** | `CE190614` — Nguyễn Tấn Quốc | Core & Infrastructure | `SearchScreen`, `NotificationScreen`, `CategoryDetailScreen`, `AdminDashboardScreen`, App Core | **Voice Search**: Microphone phần cứng, runtime `RECORD_AUDIO`, `speech_to_text` (vi_VN) → Elasticsearch |

> ⚠️ **Phân biệt rõ**: `AlbumManagementScreen` (T1) = Artist Studio CRUD (tạo/sửa album). `AlbumDetailScreen` (T4) = Listener view (xem album). Khác nhau hoàn toàn.

---

## 🔍 PHẦN 3: QUY TRÌNH PULL REQUEST & CODE REVIEW VÒNG TRÒN KÉP (DUAL-RING PEER REVIEW)

1. **Trước khi tạo PR:**
   - Chạy `flutter analyze` (phải 0 errors, 0 warnings).
   - Chạy `flutter test` (tất cả test phải Pass).
   - Rebase với branch `develop` mới nhất.

2. **Ma trận Review chéo Vòng tròn kép (Mỗi người review đúng 2 người, được 2 người review):**
   - **T1** review code của: **T2**, **T3**
   - **T2** review code của: **T3**, **T4**
   - **T3** review code của: **T4**, **T5**
   - **T4** review code của: **T5**, **T1**
   - **T5 (Leader)** review code của: **T1**, **T2**
   - *Bắt buộc phải có ít nhất 1 thành viên Approve và comment nhận xét mới được merge vào `develop`.*
   - *Tuyệt đối không tự Approve và tự Merge PR của chính mình (Self-merge).*

---

## 🤖 PHẦN 4: QUY ĐỊNH VỀ AI ASSISTANCE LOG (QUAN TRỌNG NHẤT ĐỂ TRÁNH BỊ TRỪ ĐIỂM C6)

### 4.1 Quy định sử dụng AI:
- Được phép dùng AI (ChatGPT, Claude, Gemini, Copilot) để hỗ trợ: lên khung code (boilerplate), gợi ý giải thuật, giải thích lỗi, viết mock test.
- **Bắt buộc phải hiểu 100% dòng code được đưa vào dự án** (giảng viên sẽ hỏi trực tiếp ở phần Defense 2.5đ).
- **Mọi đoạn code sinh ra từ AI đều phải được ghi lại trong AI Log.**

### 4.2 Định dạng bảng AI Log (Áp dụng cho cả Google Sheet và file Markdown):

| Ngày | Người thực hiện (MSSV) | Feature / Module | Prompt chi tiết đã gửi cho AI | Kết quả AI sinh ra | Bạn đã chỉnh sửa, tối ưu & test lại như thế nào? | Trạng thái (Passed / Failed) |
|---|---|---|---|---|---|---|
| 24/09 | CE190737 | Auth / Interceptor | "Viết Dio Interceptor tự động refresh JWT token khi gặp lỗi 401 trong Flutter" | Sinh ra class DioInterceptor có hàm onError | Sửa lại logic Queue để hứng các request đồng thời, tích hợp `FlutterSecureStorage` và viết Unit Test mock 401 | Passed (Unit Test 100%) |
| 25/09 | CE190036 | Player / Cache | "Cách cache file audio mp3 bằng LockCachingAudioSource trong just_audio" | Đoạn code LockCachingAudioSource | Tùy biến hàm fallback: nếu không có mạng thì lấy từ cache, có mạng thì vừa play vừa tải vào cache | Passed (Chạy offline mượt) |

---

## 🚀 PHẦN 5: DEFINITION OF DONE (DOD) & CHUẨN CODE (C4 & C5)

Một tính năng chỉ được xem là "HOÀN THÀNH" khi:
- [x] Chạy thực tế mượt mà trên emulator hoặc thiết bị thật (không crash).
- [x] Có đầy đủ 3 trạng thái giao diện: **Loading** (Shimmer skeleton có cấu trúc), **Error** (báo lỗi + nút Thử lại), **Data/Empty** (dữ liệu hoặc Empty State widget).
- [x] Đã hoàn thành từ **UI -> State Provider -> Repository -> API Data Source** (Vertical Slice).
- [x] **Quy chuẩn State (Tránh cực đoan / Tránh over-engineering)**:
  - Bắt buộc dùng **Riverpod** cho Business Logic, API data fetching, User/Token/Player state và chia sẻ dữ liệu liên màn hình. (Cấm dùng `setState` cho API/Domain logic).
  - Cho phép dùng **`StatefulWidget`** cho trạng thái UI cục bộ ngắn hạn (Ephemerals) như: ẩn/hiện mật khẩu trong form, đóng/mở dropdown local, `AnimationController`.
- [x] **Lộ trình Kiểm thử (Testing Roadmap)**:
  - **Tuần 2 (CP2)**: Bắt buộc có **1 Unit Test** kiểm thử chính luồng Vertical Slice của mình (Mocktail).
  - **Tuần 4 (CP4)**: Bắt buộc có thêm **1 Widget Test** kiểm thử tương tác giao diện.
- [x] Đã được log đầy đủ vào **AI Log** (Google Sheet + file Markdown).
- [x] Đã được tạo PR và duyệt bởi ít nhất 1 thành viên theo ma trận vòng tròn kép.
- [x] **UI đạt chuẩn Material 3**: không overflow trên màn hình 360dp, có Ripple/Haptic feedback, Shimmer đúng cấu trúc.

---

## 🎨 PHẦN 6: TIÊU CHUẨN UI MATERIAL 3 & QUY TẮC CHỐNG "AI SLOP" (ANTI-AI SLOP)

> **Mục tiêu**: Đảm bảo UI Music4 đạt chất lượng thương mại, tránh bị giảng viên nhận ra là giao diện "AI tạo rập khuôn".

### 6.1 Tiêu chuẩn Material 3 bắt buộc:
- **`useMaterial3: true`** trong `ThemeData` (bắt buộc).
- Dùng hệ màu ngữ nghĩa `ColorScheme` (không hardcode hex màu trực tiếp vào widget — dùng `Theme.of(context).colorScheme.primary`, `.surface`, `.onSurface`, etc.).
- Typography phải dùng `TextTheme` chuẩn M3: `displayLarge`, `titleMedium`, `bodySmall` — không tự đặt `fontSize` random.
- Dynamic elevation (`CardThemeData`, `ElevatedButton` dùng `elevation` đúng M3 level).

### 6.2 Quy tắc Anti-AI Slop (Cấm tuyệt đối):

| ❌ BỊ CẤM | ✅ THAY BẰNG |
|-----------|-------------|
| Gradient tím/xanh neon phát sáng vô nghĩa | Gradient tinh tế theo `ColorScheme` hoặc không dùng gradient |
| `BorderRadius.circular(30)` cho mọi widget | `BorderRadius` đúng theo M3: Card=12dp, Button=20dp (FilledButton M3 chuẩn) |
| `CircularProgressIndicator` đặt giữa màn hình khi load | **Shimmer Skeleton** có cấu trúc khớp với card thật |
| Padding/margin số lẻ (`padding: 13, margin: 7`) | **8-pt Grid System**: padding/margin phải là bội số của 8 (8, 16, 24, 32) |
| Không xử lý overflow — text bị tràn ra ngoài | `LayoutBuilder` + `Flexible`/`Expanded` + `TextOverflow.ellipsis` |
| Không có phản hồi chạm (tap không có feedback) | **Ripple effect** (`InkWell`/`InkResponse`), **Haptic feedback** (`HapticFeedback.lightImpact()`) |

### 6.3 Checklist UI trước khi tạo PR:
- [ ] Mở widget trên màn hình nhỏ (360dp width) — không bị overflow.
- [ ] Bấm vào mọi nút/card — có Ripple effect.
- [ ] Tắt mạng — hiển thị Error widget, không crash.
- [ ] Vào màn hình lần đầu — hiện Shimmer skeleton đúng cấu trúc, không hiện trang trắng.
- [ ] `flutter analyze` → 0 issues.
