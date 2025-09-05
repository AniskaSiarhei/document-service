package com.example.documentservice.repository;

import com.example.documentservice.entity.Document;
import com.example.documentservice.entity.DocumentShare;
import com.example.documentservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {

    /**
     * Проверяет, существует ли уже запись о расшаривании для данной пары документа и получателя.
     * @param document Документ
     * @param recipient Пользователь-получатель
     * @return true, если запись существует, иначе false
     */
    boolean existsByDocumentAndRecipient(Document document, User recipient);

    boolean existsByDocumentIdAndRecipientId(Long documentId, Long recipientId);

    @Modifying
    @Query("DELETE FROM DocumentShare ds WHERE ds.document.id = :documentId")
    void deleteAllByDocumentId(@Param("documentId") Long documentId);
}
