-- content-service | SQL Server | DB: Music4_Content
CREATE TABLE artists (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT NULL,                             -- tài khoản bên user-service (không FK chéo DB)
    name        NVARCHAR(100) NOT NULL,
    img         NVARCHAR(MAX) NULL,
    cover       NVARCHAR(MAX) NULL,
    gender      BIT NULL,
    track_total INT NOT NULL CONSTRAINT DF_artists_track_total DEFAULT 0,
    album_total INT NOT NULL CONSTRAINT DF_artists_album_total DEFAULT 0,
    is_deleted  BIT NOT NULL CONSTRAINT DF_artists_is_deleted DEFAULT 0,
    created_at  DATETIME2(6) NOT NULL CONSTRAINT DF_artists_created_at DEFAULT SYSDATETIME()
);
-- SQL Server: UNIQUE thường chỉ cho 1 giá trị NULL => dùng filtered index
CREATE UNIQUE INDEX UX_artists_user_id ON artists (user_id) WHERE user_id IS NOT NULL;

CREATE TABLE categories (
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    CONSTRAINT UQ_categories_name UNIQUE (name)
);

CREATE TABLE albums (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    img         NVARCHAR(MAX) NULL,
    upload_date DATE NOT NULL CONSTRAINT DF_albums_upload_date DEFAULT CAST(SYSDATETIME() AS DATE),
    track_total INT  NOT NULL CONSTRAINT DF_albums_track_total DEFAULT 0,
    artist_id   INT  NOT NULL CONSTRAINT FK_albums_artists REFERENCES artists (id)
);
CREATE INDEX IX_albums_artist ON albums (artist_id);

CREATE TABLE tracks (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    name         NVARCHAR(100) NOT NULL,
    img          NVARCHAR(MAX) NULL,
    duration     INT NULL CONSTRAINT CK_tracks_duration CHECK (duration IS NULL OR duration > 0),
    file_path    NVARCHAR(MAX) NULL,
    preview_path NVARCHAR(MAX) NULL,
    upload_date  DATE   NOT NULL CONSTRAINT DF_tracks_upload_date DEFAULT CAST(SYSDATETIME() AS DATE),
    view_count   BIGINT NOT NULL CONSTRAINT DF_tracks_view_count DEFAULT 0,
    album_id     INT NULL CONSTRAINT FK_tracks_albums REFERENCES albums (id)
);
CREATE INDEX IX_tracks_album ON tracks (album_id);
CREATE INDEX IX_tracks_view_count ON tracks (view_count DESC);

CREATE TABLE track_artists (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    track_id  INT NOT NULL CONSTRAINT FK_track_artists_tracks REFERENCES tracks (id) ON DELETE CASCADE,
    artist_id INT NOT NULL CONSTRAINT FK_track_artists_artists REFERENCES artists (id),
    role      NVARCHAR(50) NOT NULL CONSTRAINT DF_track_artists_role DEFAULT N'MAIN',
    CONSTRAINT UQ_track_artists UNIQUE (track_id, artist_id)
);
CREATE INDEX IX_track_artists_artist ON track_artists (artist_id);

CREATE TABLE track_categories (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    track_id    INT NOT NULL CONSTRAINT FK_track_categories_tracks REFERENCES tracks (id) ON DELETE CASCADE,
    category_id INT NOT NULL CONSTRAINT FK_track_categories_categories REFERENCES categories (id),
    CONSTRAINT UQ_track_categories UNIQUE (track_id, category_id)
);
CREATE INDEX IX_track_categories_category ON track_categories (category_id);

-- Consumer idempotent: eventId đã xử lý
CREATE TABLE processed_events (
    event_id     NVARCHAR(64) NOT NULL PRIMARY KEY,
    processed_at DATETIME2(6) NOT NULL
);
