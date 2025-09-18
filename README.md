# BookingTower - Coworking Booking System

## Описание проекта

BookingTower - это backend-система для бронирования рабочих мест в коворкинге с почасовой арендой, регистрацией пользователей и онлайн-оплатой через ЮKassa. Система включает админ-панель и REST API.

## Что уже реализовано

### ✅ Архитектура и структура проекта
- Многослойная архитектура (api, application, domain, infrastructure, config)
- Правильная структура пакетов
- Конфигурация Spring Boot 3.x с Java 17

### ✅ Доменная модель
- **User** - пользователи с ролями (USER/ADMIN), email-верификацией
- **Coworking** - коворкинг-пространства с часовыми поясами и рабочими часами
- **Workspace** - рабочие зоны с ценообразованием
- **WorkspaceSeat** - индивидуальные рабочие места
- **CalendarSlot** - временные слоты с состояниями (OPEN/HELD/BOOKED)
- **Booking** - бронирования с жизненным циклом
- **Payment** - платежи с поддержкой ЮKassa

### ✅ Слой данных
- PostgreSQL с Liquibase миграциями
- JPA репозитории с оптимизированными запросами
- Индексы для производительности
- Seed данные (админ, коворкинг, рабочие места)

### ✅ Бизнес-логика (Services)
- **UserService** - регистрация, аутентификация, email-верификация
- **BookingService** - полный цикл бронирования с HOLD механизмом
- **AvailabilityService** - проверка доступности, генерация слотов
- **EmailService** - уведомления (пока mock-реализация)

### ✅ REST API
- Контроллер для проверки доступности
- Swagger/OpenAPI документация
- Обработка ошибок и логирование

### ✅ Конфигурация
- Spring Security с BCrypt
- JPA Auditing
- Настройки приложения
- Профили для dev/prod

## Технологический стек

- **Java 17**
- **Spring Boot 3.5.5**
- **Spring Data JPA** (Hibernate)
- **Spring Security**
- **PostgreSQL 15+**
- **Liquibase** для миграций
- **JWT** для аутентификации
- **SpringDoc OpenAPI** для документации
- **SLF4J + Logback** для логирования

## Как запустить

### Предварительные требования
- Java 17+
- PostgreSQL 15+
- Gradle 7+

### Настройка базы данных
```sql
CREATE DATABASE booking_tower;
CREATE USER booking_user WITH PASSWORD 'booking_password';
GRANT ALL PRIVILEGES ON DATABASE booking_tower TO booking_user;
```

### Переменные окружения
Создайте файл `.env` или установите переменные:
```bash
DB_USERNAME=booking_user
DB_PASSWORD=booking_password
JWT_SECRET=your-secret-key-here
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
YOOKASSA_SHOP_ID=your-shop-id
YOOKASSA_SECRET_KEY=your-secret-key
```

### Запуск приложения
```bash
./gradlew bootRun
```

### Доступ к API
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

### Тестовые данные
После запуска будут созданы:
- Админ: `admin@bookingtower.com` / `password`
- Коворкинг "TechHub Moscow"
- 2 рабочие зоны (Open Space, Meeting Room)
- 16 рабочих мест

## Примеры API запросов

### Генерация слотов
```bash
curl -X POST "http://localhost:8080/api/availability/workspace/1/generate-slots?startDate=2024-01-15&endDate=2024-01-20"
```

### Проверка доступности
```bash
curl "http://localhost:8080/api/availability/workspace/1?date=2024-01-15"
```

## Что нужно доделать

### 🔄 Высокий приоритет
1. **PaymentService и YooKassa интеграция**
   - Создание платежей
   - Обработка webhook'ов
   - Возвраты средств

2. **JWT аутентификация**
   - JWT токены
   - Refresh токены
   - Middleware для авторизации

3. **Остальные REST контроллеры**
   - AuthController (регистрация, логин)
   - BookingController (бронирование)
   - PaymentController (платежи)
   - AdminController (управление)

4. **Планировщик задач**
   - Освобождение просроченных HOLD
   - Отметка no-show
   - Напоминания

### 🔄 Средний приоритет
5. **Admin веб-панель (Thymeleaf)**
   - Дашборд
   - Управление местами
   - Отчеты

6. **Email интеграция**
   - Реальная отправка писем
   - HTML шаблоны

7. **Тестирование**
   - Unit тесты
   - Integration тесты с Testcontainers

### 🔄 Низкий приоритет
8. **Docker и деплой**
   - Dockerfile
   - docker-compose
   - CI/CD

9. **Дополнительные фичи**
   - Кэширование (Redis)
   - Rate limiting
   - Метрики и мониторинг

## Структура проекта

```
src/main/java/org/example/bookingtower/
├── api/controller/          # REST контроллеры
├── application/service/     # Бизнес-логика
├── domain/entity/          # Доменные сущности
├── infrastructure/repository/ # Репозитории данных
├── config/                 # Конфигурация
└── BookingTowerApplication.java

src/main/resources/
├── db/changelog/           # Liquibase миграции
└── application.properties  # Настройки
```

## Контакты и поддержка

Система представляет собой MVP с основной функциональностью бронирования. Для production использования требуется доработка безопасности, тестирования и мониторинга.

## Лицензия

MIT License