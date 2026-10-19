-- Tài khoản demo. Mật khẩu ĐÃ HASH BCrypt (không lưu plaintext):
--   admin / Admin123     artist / Artist123     listener / Listener123
-- Thứ tự insert cố định để id = 1, 2, 3 (content-service seed artist có user_id = 2).
INSERT INTO users (name, email, password, user_name, role, artist_id) VALUES
 (N'Quản trị viên', N'admin@music4.local',    N'$2a$10$LhUKTTvWvK0w/UgAvEXGf.j8Een.RWGGMiTQWPncMkwHIs2wHLT9C', N'admin',    N'ADMIN',    NULL),
 (N'Demo Artist',   N'artist@music4.local',   N'$2a$10$sC4nc7mVoj2LsBkP5Vkjzu3xVe/Icxe2kfWYbpkMxtBm9hhoB56d.', N'artist',   N'ARTIST',   1),
 (N'Demo Listener', N'listener@music4.local', N'$2a$10$dM35RuCE/gtfHY3EmcWuC.zJdHPKvxSR0pT8jRLS4Ogf7Tu2QmU5O', N'listener', N'LISTENER', NULL);
