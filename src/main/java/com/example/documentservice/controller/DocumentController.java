package com.example.documentservice.controller;

import com.example.documentservice.dto.DocumentDto;
import com.example.documentservice.dto.FileDownloadDto;
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
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> uploadDocument(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "category", required = false) String category,
                                                      @RequestParam(value = "tags", required = false) Set<String> tags,
                                                      @AuthenticationPrincipal User user) {
        DocumentDto documentDto = documentService.uploadDocument(file, category, tags, user);
        return ResponseEntity.ok(documentDto);
    }

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

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @AuthenticationPrincipal User user) {
        // ИСПОЛЬЗУЕМ НОВЫЙ DTO
        FileDownloadDto fileDto = documentService.downloadDocument(id, user);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileDto.fileName() + "\"")
                .contentType(fileDto.mediaType())
                .body(fileDto.resource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id,
                                               @AuthenticationPrincipal User user) throws Exception {
        documentService.deleteDocument(id, user);
        return ResponseEntity.noContent().build();
    }
}
