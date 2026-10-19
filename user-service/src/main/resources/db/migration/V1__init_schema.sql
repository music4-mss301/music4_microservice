-- user-service | SQL Server | DB: Music4_User
CREATE TABLE users (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    img         NVARCHAR(MAX) NULL,
    name        NVARCHAR(100) NOT NULL,
    email       NVARCHAR(100) NOT NULL,
    password    NVARCHAR(255) NOT NULL,               -- luôn là BCrypt hash
    user_name   NVARCHAR(50)  NOT NULL,
    role        NVARCHAR(20)  NOT NULL CONSTRAINT DF_users_role DEFAULT N'LISTENER',
    artist_id   INT NULL,                             -- id hồ sơ bên content-service (không FK chéo DB)
    gender      BIT NULL,
    is_deleted  BIT NOT NULL CONSTRAINT DF_users_is_deleted DEFAULT 0,
    enabled     BIT NOT NULL CONSTRAINT DF_users_enabled DEFAULT 1,
    created_at  DATETIME2(6) NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_users_user_name UNIQUE (user_name),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT CK_users_role CHECK (role IN (N'LISTENER', N'ARTIST', N'ADMIN'))
);

CREATE TABLE refresh_tokens (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    token       NVARCHAR(255) NOT NULL,
    username    NVARCHAR(255) NOT NULL,
    source      NVARCHAR(255) NOT NULL,
    expiry_date DATETIME2(6)  NOT NULL,
    revoked     BIT NOT NULL CONSTRAINT DF_refresh_tokens_revoked DEFAULT 0,
    CONSTRAINT UQ_refresh_tokens_token UNIQUE (token)
);
CREATE INDEX IX_refresh_tokens_username ON refresh_tokens (username);

CREATE TABLE password_reset_tokens (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    token        NVARCHAR(255) NOT NULL,
    email        NVARCHAR(255) NOT NULL,
    account_type NVARCHAR(255) NOT NULL,
    expiry_date  DATETIME2(6)  NOT NULL,
    used         BIT NOT NULL CONSTRAINT DF_password_reset_tokens_used DEFAULT 0,
    CONSTRAINT UQ_password_reset_tokens_token UNIQUE (token)
);
