package com.example.documentservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfiguration {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access.key}")
    private String accessKey;

    @Value("${minio.secret.key}")
    private String secretKey;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient;
        try {
            log.info("Attempting to connect to MinIO at URL: {}", url);
            minioClient = MinioClient.builder()
                    .endpoint(url)
                    .credentials(accessKey, secretKey)
                    .build();
            log.info("MinIO client created successfully.");

            // Проверяем, существует ли bucket.
            log.info("Checking if bucket '{}' exists...", bucketName);
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Если нет, создаем его.
                log.info("Bucket '{}' not found, creating it...", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created successfully.", bucketName);
            } else {
                log.info("MinIO bucket '{}' already exists.", bucketName);
            }
        } catch (Exception e) {
            // --- УЛУЧШЕННОЕ ЛОГИРОВАНИЕ ОШИБКИ ---
            log.error("FATAL: Could not initialize MinIO client. This is likely a network or configuration issue.", e);
            log.error("Please check the following:");
            log.error("1. Is the MinIO Docker container running? (Use 'docker ps')");
            log.error("2. Is the minio.url in application.properties ('{}') correct and accessible from the application?", url);
            log.error("3. Are the access/secret keys correct?");
            // Перевыбрасываем исключение, чтобы Spring остановил запуск
            throw new RuntimeException("Failed to initialize MinIO client. Check logs for details.", e);
        }

        return minioClient;
    }
}
