-- analytics-service | SQL Server | DB: Music4_Analytics
CREATE TABLE history (
    id                INT IDENTITY(1,1) PRIMARY KEY,
    user_id           INT NULL,                         -- NULL = khách
    track_id          INT NOT NULL,
    listened_at       DATETIME2(6) NOT NULL CONSTRAINT DF_history_listened_at DEFAULT SYSDATETIME(),
    playback_position INT NOT NULL CONSTRAINT DF_history_position DEFAULT 0
                      CONSTRAINT CK_history_position CHECK (playback_position >= 0)
);
CREATE INDEX IX_history_user_track_time ON history (user_id, track_id, listened_at DESC);
CREATE INDEX IX_history_track_time ON history (track_id, listened_at);
CREATE INDEX IX_history_time ON history (listened_at);
