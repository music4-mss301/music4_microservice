-- DÙNG KHI CHẠY PostgreSQL CÀI TRÊN MÁY (không dùng Neon). Chạy một lần:
--   psql -U postgres -f create-databases.sql
-- Với Neon: tạo 5 database trong giao diện (Neon Console > Databases > New database), không cần file này.
CREATE DATABASE music4_user;
CREATE DATABASE music4_content;
CREATE DATABASE music4_engagement;
CREATE DATABASE music4_analytics;
CREATE DATABASE music4_media;
