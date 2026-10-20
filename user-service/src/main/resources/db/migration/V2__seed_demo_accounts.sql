-- Tài khoản demo. Mật khẩu ĐÃ HASH BCrypt (không lưu plaintext):
--   admin / Admin123     artist / Artist123     listener / Listener123
-- Thứ tự insert cố định để id = 1, 2, 3 (content-service seed artist có user_id = 2).
INSERT INTO users (name, email, password, user_name, role, artist_id) VALUES
 ('Quản trị viên', 'admin@music4.local',    '$2a$10$LhUKTTvWvK0w/UgAvEXGf.j8Een.RWGGMiTQWPncMkwHIs2wHLT9C', 'admin',    'ADMIN',    NULL),
 ('Demo Artist',   'artist@music4.local',   '$2a$10$sC4nc7mVoj2LsBkP5Vkjzu3xVe/Icxe2kfWYbpkMxtBm9hhoB56d.', 'artist',   'ARTIST',   1),
 ('Demo Listener', 'listener@music4.local', '$2a$10$dM35RuCE/gtfHY3EmcWuC.zJdHPKvxSR0pT8jRLS4Ogf7Tu2QmU5O', 'listener', 'LISTENER', NULL);
