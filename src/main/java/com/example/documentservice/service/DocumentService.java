package com.example.documentservice.service;

import com.example.documentservice.dto.FileDownloadDto;
import com.example.documentservice.dto.DocumentDto;
import com.example.documentservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface DocumentService {

    DocumentDto uploadDocument(MultipartFile file, String category, Set<String> tags, User owner);

    FileDownloadDto downloadDocument(Long id, User user);

    void deleteDocument(Long id, User user);

    Page<DocumentDto> getAllUserDocuments(User owner, String category, Set<String> tags, String query, Pageable pageable);

    Page<DocumentDto> getAllDocumentsForAdmin(Pageable pageable, String category, Set<String> tags, String query, String username);

    /**
     * Расшаривает документ другому пользователю.
     * @param documentId ID документа для расшаривания
     * @param recipientUsername Имя пользователя-получателя
     * @param sender Пользователь, который инициирует действие
     */
    void shareDocument(Long documentId, String recipientUsername, User sender);

    /**
     * Получает страницу документов, которые были расшарены текущему пользователю.
     * @param currentUser Текущий аутентифицированный пользователь
     * @param pageable    Информация о пагинации
     * @return Страница с DTO документов
     */
    Page<DocumentDto> getSharedWithMe(User currentUser, Pageable pageable);

    /**
     * Сохраняет копию расшаренного документа для текущего пользователя.
     * @param sourceDocumentId ID исходного документа
     * @param currentUser      Пользователь, который сохраняет копию
     * @return DTO нового, сохраненного документа
     */
    DocumentDto saveSharedDocument(Long sourceDocumentId, User currentUser) throws Exception;
}