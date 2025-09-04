package com.example.documentservice.service;

import com.example.documentservice.dto.FileDownloadDto;
import com.example.documentservice.entity.DocumentShare;
import com.example.documentservice.entity.Role;
import com.example.documentservice.repository.DocumentShareRepository;
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
import java.util.HashSet;
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
    private final DocumentShareRepository documentShareRepository;

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
    public void shareDocument(Long documentId, String recipientUsername, User sender) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));

        // Проверка прав: только владелец может расшарить документ
        if (!document.getOwner().getId().equals(sender.getId())) {
            throw new AccessDeniedException("Only the owner of the document can share it. You do not have permission to share this document");
        }

        // Проверка наличия пользователя в базе данных
        User recipient = userRepository.findByUsername(recipientUsername)
                .orElseThrow(() -> new EntityNotFoundException("Recipient user not found with username: " + recipientUsername));

        // Проверка на расшаривание самому себе
        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("You cannot share a document with yourself");
        }

        // Проверка, не был ли документ уже расшарен этому пользователю
        if (documentShareRepository.existsByDocumentAndRecipient(document, recipient)) {
            throw new IllegalArgumentException("Document is already shared with user: " + recipientUsername);
        }

        // Создание и сохранение записи о праве доступа
        DocumentShare documentShare = DocumentShare.builder()
                .document(document)
                .recipient(recipient)
                .build();

        documentShareRepository.save(documentShare);

        log.info("User '{}' shared document '{}' (ID: {}) with user '{}'",
                sender.getUsername(), document.getFileName(), document.getId(), recipientUsername);
    }

    @Override
    public Page<DocumentDto> getSharedWithMe(User currentUser, Pageable pageable) {

        Page<Document> documents = documentRepository.findDocumentsSharedWithUser(currentUser.getId(), pageable);
        return documents.map(this::mapToDto);
    }

    @Override
    @Transactional
    public DocumentDto saveSharedDocument(Long sourceDocumentId, User currentUser) throws Exception {
        // 1. Проверяем, что исходный документ существует
        Document sourceDocument = documentRepository.findById(sourceDocumentId)
                .orElseThrow(() -> new EntityNotFoundException("Source document not found with id: " + sourceDocumentId));

        // 2. Проверка безопасности: убеждаемся, что документ действительно расшарен этому пользователю
        if (!documentShareRepository.existsByDocumentIdAndRecipientId(sourceDocumentId, currentUser.getId())) {
            // Дополнительная проверка: может пользователь пытается сохранить свой же документ из чужого списка?
            if (!sourceDocument.getOwner().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to access this document.");
            }
        }

        // 3. Проверка логики: нельзя сохранить свой же документ
        if (sourceDocument.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You cannot save a document you already own.");
        }

        // 4. Копируем файл в MinIO
        String newStorageFileName = fileStorageService.copyFile(sourceDocument.getStorageFileName());

        // 5. Создаем новую запись в БД
        Document newDocument = Document.builder()
                .fileName(sourceDocument.getFileName())
                .fileType(sourceDocument.getFileType())
                .size(sourceDocument.getSize())
                .category(sourceDocument.getCategory())
                .tags(sourceDocument.getTags() != null ? new java.util.HashSet<>(sourceDocument.getTags()) : new java.util.HashSet<>())
                .storageFileName(newStorageFileName)
                .owner(currentUser) // <-- Новый владелец!
                .build();

        Document savedDocument = documentRepository.save(newDocument);
        log.info("User '{}' saved a copy of document '{}' (Source ID: {}, New ID: {})",
                currentUser.getUsername(), savedDocument.getFileName(), sourceDocumentId, savedDocument.getId());

        return mapToDto(savedDocument);
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