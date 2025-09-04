package com.example.documentservice.repository;

import com.example.documentservice.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    @Query("SELECT ds.document FROM DocumentShare ds WHERE ds.recipient.id = :userId")
    Page<Document> findDocumentsSharedWithUser(@Param("userId") Long userId, Pageable pageable);
}
