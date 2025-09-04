package com.example.documentservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String storageFileName;

    @Column(nullable = false, unique = true)
    private String fileType;

    @Column(nullable = false)
    private long size;

    private String category;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime uploadDate;

    @ElementCollection(fetch = FetchType.EAGER)  // EAGER - загружать теги вместе с документом
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id")) // Таблица для хранения тегов
    @Column(name = "tag")
    private Set<String> tags;

    // Связь "Многие к одному": много документов могут принадлежать одному пользователю
    @ManyToOne(fetch = FetchType.LAZY) // LAZY - загружать пользователя только при прямом обращении
    @JoinColumn(name = "user_id", nullable = false) // Внешний ключ на таблицу users
    private User owner;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentShare> shares = new HashSet<>();
}
