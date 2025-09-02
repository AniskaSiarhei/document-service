package com.example.documentservice.dto;

import com.example.documentservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    private Long id;
    private String fileName;
    private String fileType;
    private long size;
    private String category;
    private LocalDateTime uploadDate;
    private Set<String> tags;

    private String ownerUsername;
}
















