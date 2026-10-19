/* ============================================================================
   Music4 - khởi tạo SQL Server trong Docker (chạy 1 lần bởi container sqlserver-init)
   - 5 database riêng cho 5 service (DB per service)
   - 5 login riêng, mỗi login CHỈ có quyền trên DB của mình (db_owner để Flyway tạo bảng)
   Bảng do Flyway của từng service tạo, script này KHÔNG tạo bảng.
   Biến sqlcmd: $(SVC_PASSWORD) truyền từ docker-compose (-v SVC_PASSWORD=...)
   ============================================================================ */
SET NOCOUNT ON;

DECLARE @dbs TABLE (db SYSNAME, login SYSNAME);
INSERT INTO @dbs VALUES
 (N'Music4_User',       N'music4_group4'),
 (N'Music4_Content',    N'music4_group4'),
 (N'Music4_Engagement', N'music4_group4'),
 (N'Music4_Analytics',  N'music4_group4'),
 (N'Music4_Media',      N'music4_group4');

DECLARE @db SYSNAME, @login SYSNAME, @sql NVARCHAR(MAX);
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT db, login FROM @dbs;
OPEN c;
FETCH NEXT FROM c INTO @db, @login;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF DB_ID(@db) IS NULL
    BEGIN
        SET @sql = N'CREATE DATABASE ' + QUOTENAME(@db) + N' COLLATE Vietnamese_CI_AS;';
        EXEC (@sql);
        PRINT N'Created database ' + @db;
    END

    IF SUSER_ID(@login) IS NULL
    BEGIN
        SET @sql = N'CREATE LOGIN ' + QUOTENAME(@login) + N' WITH PASSWORD = N''$(SVC_PASSWORD)'', '
                 + N'DEFAULT_DATABASE = ' + QUOTENAME(@db) + N', CHECK_POLICY = OFF;';
        EXEC (@sql);
        PRINT N'Created login ' + @login;
    END

    SET @sql = N'USE ' + QUOTENAME(@db) + N';
        IF USER_ID(N''' + @login + N''') IS NULL
            CREATE USER ' + QUOTENAME(@login) + N' FOR LOGIN ' + QUOTENAME(@login) + N';
        ALTER ROLE db_owner ADD MEMBER ' + QUOTENAME(@login) + N';';
    EXEC (@sql);

    FETCH NEXT FROM c INTO @db, @login;
END
CLOSE c;
DEALLOCATE c;
PRINT N'Music4 SQL Server init done';
GO
