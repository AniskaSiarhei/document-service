package com.example.documentservice.repository;

import com.example.documentservice.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    @Query("SELECT ds.document FROM DocumentShare ds WHERE ds.recipient.id = :userId")
    Page<Document> findDocumentsSharedWithUser(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTINCT d FROM Document d " +
                   "JOIN d.shares s " +
                   "WHERE s.recipient.id = :userId")
    Page<Document> findSharedWithUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * ДОБАВЛЕННЫЙ МЕТОД: Находит документ по ID и принудительно загружает
     * связанные с ним 'shares' одним запросом, чтобы избежать LazyInitializationException.
     * @param id ID документа
     * @return Optional с документом и его 'shares'
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.shares WHERE d.id = :id")
    Optional<Document> findByIdWithShares(@Param("id") Long id);
}
