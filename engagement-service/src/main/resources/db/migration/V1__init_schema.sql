-- engagement-service | SQL Server | DB: Music4_Engagement
-- user_id / track_id / artist_id là id của service khác: KHÔNG có FK chéo DB, chỉ có index.
CREATE TABLE playlists (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    user_id     INT NOT NULL,
    description NVARCHAR(500) NULL,
    img         NVARCHAR(MAX) NULL,
    is_public   BIT NOT NULL CONSTRAINT DF_playlists_is_public DEFAULT 1,
    created_at  DATETIME2(6) NOT NULL CONSTRAINT DF_playlists_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_playlists_user_name UNIQUE (user_id, name)
);

CREATE TABLE playlist_tracks (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    playlist_id INT NOT NULL CONSTRAINT FK_playlist_tracks_playlists REFERENCES playlists (id) ON DELETE CASCADE,
    track_id    INT NOT NULL,
    added_at    DATETIME2(6) NOT NULL CONSTRAINT DF_playlist_tracks_added_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_playlist_tracks UNIQUE (playlist_id, track_id)
);
CREATE INDEX IX_playlist_tracks_track ON playlist_tracks (track_id);

CREATE TABLE favorites (
    id       INT IDENTITY(1,1) PRIMARY KEY,
    user_id  INT NOT NULL,
    track_id INT NOT NULL,
    liked_at DATETIME2(6) NOT NULL CONSTRAINT DF_favorites_liked_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_favorites_user_track UNIQUE (user_id, track_id)
);
CREATE INDEX IX_favorites_track ON favorites (track_id);

CREATE TABLE follows (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT NOT NULL,
    artist_id   INT NOT NULL,
    followed_at DATETIME2(6) NOT NULL CONSTRAINT DF_follows_followed_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_follows_user_artist UNIQUE (user_id, artist_id)
);
CREATE INDEX IX_follows_artist ON follows (artist_id);

CREATE TABLE ratings (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT NOT NULL,
    track_id     INT NOT NULL,
    rating_value INT NOT NULL CONSTRAINT CK_ratings_value CHECK (rating_value BETWEEN 1 AND 5),
    updated_at   DATETIME2(6) NOT NULL CONSTRAINT DF_ratings_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_ratings_user_track UNIQUE (user_id, track_id)
);
CREATE INDEX IX_ratings_track ON ratings (track_id);

CREATE TABLE comments (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    track_id   INT NOT NULL,
    user_id    INT NOT NULL,
    content    NVARCHAR(500) NOT NULL,
    created_at DATETIME2(6) NOT NULL CONSTRAINT DF_comments_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(6) NOT NULL CONSTRAINT DF_comments_updated_at DEFAULT SYSDATETIME()
);
CREATE INDEX IX_comments_track ON comments (track_id, created_at DESC);
CREATE INDEX IX_comments_user ON comments (user_id, created_at DESC);

CREATE TABLE notifications (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT NOT NULL,
    artist_id  INT NULL,
    track_id   INT NULL,
    content    NVARCHAR(500) NOT NULL,
    is_read    BIT NOT NULL CONSTRAINT DF_notifications_is_read DEFAULT 0,
    created_at DATETIME2(6) NOT NULL CONSTRAINT DF_notifications_created_at DEFAULT SYSDATETIME()
);
CREATE INDEX IX_notifications_user ON notifications (user_id, created_at DESC);

CREATE TABLE processed_events (
    event_id     NVARCHAR(64) NOT NULL PRIMARY KEY,
    processed_at DATETIME2(6) NOT NULL
);
