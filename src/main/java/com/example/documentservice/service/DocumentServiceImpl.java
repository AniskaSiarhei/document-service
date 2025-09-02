package com.example.documentservice.service;

import com.example.documentservice.dto.FileDownloadDto;
import com.example.documentservice.entity.Role;
import com.example.documentservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import com.example.documentservice.dto.DocumentDto;
import com.example.documentservice.entity.Document;
import com.example.documentservice.entity.User;
import com.example.documentservice.repository.DocumentRepository;
import com.example.documentservice.repository.specification.DocumentSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentDto> getAllDocumentsForAdmin(Pageable pageable, String category, Set<String> tags, String query, String username) {
        Specification<Document> spec = (root, q, criteriaBuilder) -> criteriaBuilder.conjunction();

        // И добавляем к ней условия
        if (StringUtils.hasText(category)) {
            spec = spec.and(DocumentSpecification.byCategory(category));
        }
        if (tags != null && !tags.isEmpty()) {
            spec = spec.and(DocumentSpecification.byTags(tags));
        }
        if (StringUtils.hasText(query)) {
            spec = spec.and(DocumentSpecification.byFileName(query));
        }
        if (StringUtils.hasText(username)) {
            spec = spec.and(DocumentSpecification.byUsername(username));
        }

        return documentRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    public DocumentDto uploadDocument(MultipartFile file, String category, Set<String> tags, User owner) {
        // Проверяем, существует ли пользователь (на всякий случай)
        User managedOwner = userRepository.findById(owner.getId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String storageFileName = UUID.randomUUID() + "-" + originalFileName;

        // Сохраняем файл в MinIO
        fileStorageService.uploadFile(file, storageFileName);

        // Создаем и сохраняем метаданные в PostgreSQL
        Document document = Document.builder()
                .fileName(originalFileName)
                .storageFileName(storageFileName)
                .fileType(file.getContentType())
                .size(file.getSize())
                .category(category)
                .tags(tags)
                .owner(managedOwner)
                .build();

        Document savedDocument = documentRepository.save(document);

        // Преобразуем сохраненную сущность в DTO и возвращаем ее
        return mapToDto(savedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentDto> getAllUserDocuments(User owner, String category, Set<String> tags, String fileName, Pageable pageable) {
        Specification<Document> spec = DocumentSpecification.byOwnerId(owner.getId());

        if (StringUtils.hasText(category)) {
            spec = spec.and(DocumentSpecification.byCategory(category));
        }
        if (tags != null && !tags.isEmpty()) {
            spec = spec.and(DocumentSpecification.byTags(tags));
        }
        if (fileName != null && !fileName.isBlank()) {
            spec = spec.and(DocumentSpecification.byFileName(fileName));
        }

        return documentRepository.findAll(spec, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadDto downloadDocument(Long id, User user) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Документ с id " + id + " не найден."));

        // Проверка прав доступа
        if (!document.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("У вас нет прав для доступа к этому документу.");
        }

        // Получаем файл из хранилища
        InputStream fileStream = fileStorageService.downloadFile(document.getStorageFileName());
        Resource resource = new InputStreamResource(fileStream);

        // Определяем MediaType
        MediaType mediaType = MediaType.parseMediaType(document.getFileType());

        // Возвращаем новый, типизированный DTO
        return new FileDownloadDto(document.getFileName(), resource, mediaType);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, User user) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));

        // Проверяем права доступа

        boolean isAdmin = user.getRole().equals(Role.ROLE_ADMIN);
        boolean isOwner = document.getOwner().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to delete this document");
        }

        // 1. Удаляем файл из MinIO
        fileStorageService.deleteFile(document.getStorageFileName());

        // 2. Удаляем метаданные из PostgreSQL
        documentRepository.delete(document);
        log.info("User '{}' deleted document '{}' (ID: {})", user.getUsername(), document.getFileName(), id);
    }

    private DocumentDto mapToDto(Document document) {
        return DocumentDto.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .size(document.getSize())
                .category(document.getCategory())
                .uploadDate(document.getUploadDate())
                .tags(document.getTags())
                .ownerUsername(document.getOwner().getUsername())
                .build();
    }
}