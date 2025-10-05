-- Создание последовательностей для автоинкремента ID
-- Flyway будет управлять созданием таблиц, поэтому нам не нужны последовательности,
-- если мы используем IDENTITY стратегию в PostgreSQL.
-- Оставим этот файл как пример, но основной код будет ниже.

-- V1: Создание начальной схемы базы данных

-- Таблица пользователей
CREATE TABLE users
(
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    role     VARCHAR(255)
);

-- Таблица документов
CREATE TABLE documents
(
    id                BIGSERIAL PRIMARY KEY,
    file_name         VARCHAR(255) NOT NULL,
    storage_file_name VARCHAR(255) NOT NULL,
    file_type         VARCHAR(255) NOT NULL,
    size              BIGINT       NOT NULL,
    category          VARCHAR(255),
    upload_date       TIMESTAMP    NOT NULL,
    user_id           BIGINT       NOT NULL,
    CONSTRAINT fk_documents_on_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Таблица тегов для документов (связь многие-ко-многим)
CREATE TABLE document_tags
(
    document_id BIGINT       NOT NULL,
    tag         VARCHAR(255),
    CONSTRAINT fk_document_tags_on_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

-- Таблица для расшаренных документов (прав доступа)
CREATE TABLE document_shares
(
    id           BIGSERIAL PRIMARY KEY,
    document_id  BIGINT    NOT NULL,
    recipient_id BIGINT    NOT NULL,
    share_at     TIMESTAMP NOT NULL,
    CONSTRAINT fk_document_shares_on_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_document_shares_on_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    -- Уникальное ограничение, чтобы нельзя было расшарить один и тот же документ одному пользователю дважды
    CONSTRAINT uk_document_recipient UNIQUE (document_id, recipient_id)
);
