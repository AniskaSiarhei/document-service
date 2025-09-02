package com.example.documentservice.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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
    public void deleteFile(String storageFileName) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageFileName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error deleting file from MinIO", e);
            throw new RuntimeException("Error deleting file from MinIO", e);
        }
    }
}
