package com.example.documentservice.repository.specification;

import com.example.documentservice.entity.Document;
import com.example.documentservice.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class DocumentSpecification {

    /**
     * Создает спецификацию для поиска по ID владельца.
     */
    public static Specification<Document> byOwnerId(Long ownerId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("owner").get("id"), ownerId);
    }

    /**
     * Создает спецификацию для фильтрации по категории.
     */
    public static Specification<Document> byCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null; // Если категория не указана, не добавляем это условие
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category"), category);
    }

    /**
     * Создает спецификацию для фильтрации по тегам.
     * Документ должен содержать ХОТЯ БЫ ОДИН из указанных тегов.
     */
    public static Specification<Document> byTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null; // Если теги не указаны, не добавляем это условие
        }
        return (root, query, criteriaBuilder) ->
                root.join("tags").in(tags);
    }

    /**
     * Создает спецификацию для поиска по части имени файла (без учета регистра).
     */
    public static Specification<Document> byFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null; // Если имя файла не указано, не добавляем это условие
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("fileName")), "%" + fileName.toLowerCase() + "%");
    }

    // --- НОВЫЙ МЕТОД ДЛЯ АДМИНА ---
    public static Specification<Document> byUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return (root, query, criteriaBuilder) -> {
            Join<Document, User> owner = root.join("owner");
            return criteriaBuilder.like(criteriaBuilder.lower(owner.get("username")), "%" + username.toLowerCase() + "%");
        };
    }
}

