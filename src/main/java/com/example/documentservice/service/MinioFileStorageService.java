package com.example.documentservice.service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import io.minio.errors.ErrorResponseException;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public void uploadFile(MultipartFile file, String storageFileName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageFileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("File '{}' uploaded successfully to MinIO as '{}'", file.getOriginalFilename(), storageFileName);
        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Error uploading file to MinIO", e);
        }
    }

    @Override
    public InputStream downloadFile(String storageFileName) {

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageFileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file from MinIO", e);
            throw new RuntimeException("Error downloading file from MinIO", e);
        }
    }

    @Override
    public void deleteFile(String storageFileName) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageFileName)
                            .build()
            );
            log.info("Successfully deleted file '{}' from MinIO.", storageFileName);
        } catch (ErrorResponseException e) {
            // "Прощаем" ошибку, если она связана с тем, что файл не найден.
            // "NoSuchKey" - это стандартный код ошибки для S3-совместимых хранилищ.
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                log.warn("Attempted to delete file '{}', but it was not found in MinIO. " +
                         "Proceeding to delete the database record.", storageFileName);
                // Ничего не делаем, просто игнорируем эту ошибку
            } else {
                // Если ошибка другая (например, нет доступа), то ее нужно пробросить дальше
                log.error("An unexpected error occurred while deleting file '{}' from MinIO.", storageFileName, e);
                throw e;
            }
        } catch (Exception e) {
            // Ловим другие возможные исключения (сетевые и т.д.)
            log.error("An unexpected error occurred while deleting file '{}' from MinIO.", storageFileName, e);
            throw e;
        }
    }

    @Override
    public String copyFile(String sourceObjectName) throws Exception {
        String newObjectName = UUID.randomUUID() + "-" + sourceObjectName.substring(sourceObjectName.indexOf("-") + 1);

        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(newObjectName)
                        .source(
                                CopySource.builder()
                                        .bucket(bucketName)
                                        .object(sourceObjectName)
                                        .build()
                        )
                        .build()
        );
        log.info("Successfully copied file '{}' to '{}'", sourceObjectName, newObjectName);
        return newObjectName;
    }
}
