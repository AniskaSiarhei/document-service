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








}