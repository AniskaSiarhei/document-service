package com.example.documentservice.controller;

import com.example.documentservice.dto.DocumentDto;
import com.example.documentservice.dto.FileDownloadDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import com.example.documentservice.entity.User;
import com.example.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "2. Document API", description = "API for working with documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Загрузить новый документ",
            description = "Загружает файл, сохраняет его в хранилище и создает запись с метаданными в базе.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> uploadDocument(@Parameter(description = "Файл для загрузки", required = true) @RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "category", required = false) String category,
                                                      @RequestParam(value = "tags", required = false) Set<String> tags,
                                                      @AuthenticationPrincipal User user) {
        DocumentDto documentDto = documentService.uploadDocument(file, category, tags, user);
        return ResponseEntity.ok(documentDto);
    }

    @Operation(summary = "Получить список документов пользователя",
            description = "Возвращает постраничный список документов текущего пользователя с возможностью фильтрации и поиска.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список документов получен"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            })
    @GetMapping
    public ResponseEntity<Page<DocumentDto>> getUserDocuments(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentDto> documents = documentService.getAllUserDocuments(user, category, tags, query, pageable);
        return ResponseEntity.ok(documents);
    }

    @Operation(summary = "Скачать документ по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно отдан"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Документ не найден")
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@Parameter(description = "ID документа для скачивания") @PathVariable Long id,
                                                     @AuthenticationPrincipal User user) {

        FileDownloadDto fileDto = documentService.downloadDocument(id, user);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileDto.fileName() + "\"")
                .contentType(fileDto.mediaType())
                .body(fileDto.resource());
    }

    @Operation(summary = "Удалить документ по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Документ успешно удален"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
            @ApiResponse(responseCode = "404", description = "Документ не найден")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@Parameter(description = "ID документа для удаления") @PathVariable Long id,
                                               @AuthenticationPrincipal User user) throws Exception {
        documentService.deleteDocument(id, user);
        return ResponseEntity.noContent().build();
    }
}
