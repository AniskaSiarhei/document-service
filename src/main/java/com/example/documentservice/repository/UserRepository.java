package com.example.documentservice.repository;

import com.example.documentservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Объявляем, что это Spring Data репозиторий
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Ищет пользователя по его имени (username).
     * Spring Data JPA автоматически сгенерирует реализацию этого метода
     * на основе его названия.
     *
     * @param username Имя пользователя для поиска.
     * @return Optional, содержащий пользователя, если он найден, иначе пустой.
     */
    Optional<User> findByUsername(String username);
}
