-- Thông báo lưu sẵn tên bài hát / album / nghệ sĩ (các tên này thuộc content-service nên không join được)
ALTER TABLE notifications ADD COLUMN album_id    INTEGER;
ALTER TABLE notifications ADD COLUMN track_name  VARCHAR(255);
ALTER TABLE notifications ADD COLUMN album_name  VARCHAR(255);
ALTER TABLE notifications ADD COLUMN artist_name VARCHAR(255);
