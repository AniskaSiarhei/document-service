package com.example.documentservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    /**
     * Загружает файл в хранилище.
     * @param file Файл для загрузки.
     * @param storageFileName Уникальное имя файла в хранилище.
     */
    void uploadFile(MultipartFile file, String storageFileName);

    /**
     * Скачивает файл из хранилища.
     * @param storageFileName Уникальное имя файла в хранилище.
     * @return InputStream с содержимым файла.
     */
    InputStream downloadFile(String storageFileName);

    /**
     * Удаляет файл из хранилища.
     * @param storageFileName Уникальное имя файла в хранилище.
     */
    void deleteFile(String storageFileName) throws Exception;

    /**
     * Копирует файл внутри хранилища.
     * @param sourceObjectName Имя исходного файла
     * @return Имя нового, скопированного файла
     * @throws Exception если произошла ошибка при копировании
     */
    String copyFile(String sourceObjectName) throws Exception;
}
