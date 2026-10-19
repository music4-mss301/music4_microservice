-- media-service | SQL Server | DB: Music4_Media
CREATE TABLE media_files (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    object_key        NVARCHAR(500) NOT NULL,
    original_filename NVARCHAR(255) NULL,
    content_type      NVARCHAR(255) NULL,
    media_type        NVARCHAR(50)  NOT NULL CONSTRAINT CK_media_files_type CHECK (media_type IN (N'IMAGE', N'AUDIO')),
    file_size         BIGINT NULL,
    bucket_name       NVARCHAR(255) NOT NULL,
    duration          INT NULL,
    uploaded_by       INT NULL,
    created_at        DATETIME2(6) NOT NULL CONSTRAINT DF_media_files_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_media_files_object_key UNIQUE (object_key)
);
CREATE INDEX IX_media_files_type ON media_files (media_type, id DESC);
