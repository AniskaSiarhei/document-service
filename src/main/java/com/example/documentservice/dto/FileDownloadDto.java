package com.example.documentservice.dto;

import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;

/**
 * DTO, содержащий все необходимое для скачивания файла.
 * Используем record для создания простого и неизменяемого объекта.
 *
 * @param fileName Имя файла.
 * @param resource Содержимое файла.
 * @param mediaType MIME-тип файла.
 */
public record FileDownloadDto(
        String fileName,
        Resource resource,
        MediaType mediaType
) {
}
