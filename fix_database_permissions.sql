-- Скрипт для исправления прав доступа к базе данных BookingTower
-- Выполните эти команды как суперпользователь PostgreSQL (postgres)

-- 1. Подключитесь к PostgreSQL как суперпользователь
-- psql -U postgres

-- 2. Пересоздайте базу данных и пользователя с правильными правами
DROP DATABASE IF EXISTS booking_tower;
DROP USER IF EXISTS booking_user;

-- 3. Создайте пользователя с правами на создание баз данных
CREATE USER booking_user WITH PASSWORD 'booking_password' CREATEDB;

-- 4. Создайте базу данных с владельцем booking_user
CREATE DATABASE booking_tower OWNER booking_user;

-- 5. Предоставьте все права на базу данных
GRANT ALL PRIVILEGES ON DATABASE booking_tower TO booking_user;

-- 6. Подключитесь к базе данных booking_tower
\c booking_tower

-- 7. Предоставьте права на схему public
GRANT ALL ON SCHEMA public TO booking_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO booking_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO booking_user;

-- 8. Установите права по умолчанию для будущих объектов
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO booking_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO booking_user;

-- 9. Сделайте booking_user владельцем схемы public (опционально)
ALTER SCHEMA public OWNER TO booking_user;

-- Готово! Теперь можно запускать приложение