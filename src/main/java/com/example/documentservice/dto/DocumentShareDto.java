package com.example.documentservice.dto;

import java.time.LocalDateTime;

/**
 * DTO для отображения информации об отправленном документе.
 * @param shareId ID самого права доступа (для операции отзыва).
 * @param documentDto Информация о документе.
 * @param recipientUsername Имя пользователя, которому был отправлен документ.
 * @param sharedAt Дата и время отправки документа.
 */
public record DocumentShareDto(
        Long shareId,
        DocumentDto documentDto,
        String recipientUsername,
        LocalDateTime sharedAt
) {
}
