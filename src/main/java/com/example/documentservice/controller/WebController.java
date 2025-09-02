package com.example.documentservice.controller;

import com.example.documentservice.dto.DocumentDto;
import com.example.documentservice.dto.FileDownloadDto;
import com.example.documentservice.dto.SignUpRequest;
import com.example.documentservice.entity.User;
import com.example.documentservice.service.AuthenticationService;
import com.example.documentservice.service.DocumentService;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class WebController {

    private final DocumentService documentService;
    private final AuthenticationService authenticationService;


    @GetMapping()
    public String redirectToLogin() {
        return "redirect:/login";
    }
    // --- Страницы аутентификации ---

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Возвращает templates/login.html
    }

    // Мы можем использовать наш API эндпоинт для регистрации, но сделаем отдельную страницу для удобства
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("signUpRequest", new SignUpRequest());
        return "register"; // Возвращает templates/register.html
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute SignUpRequest signUpRequest, RedirectAttributes redirectAttributes) {
        try {
            authenticationService.signup(signUpRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Регистрация прошла успешно! Теперь вы можете войти.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка регистрации: " + e.getMessage());
            return "redirect:/register";
        }
    }

    // --- Основной функционал документов ---

    @GetMapping("/web/documents")
    public String documentsPage(Model model,
                                @AuthenticationPrincipal User user,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String query,
                                @RequestParam(defaultValue = "0") int page, // Номер страницы (начинается с 0)
                                @RequestParam(defaultValue = "10") int size) { // Количество элементов на странице

        // Создаем объект Pageable
        Pageable pageable = PageRequest.of(page, size);

        // Получаем страницу документов
        Page<DocumentDto> documentPage = documentService.getAllUserDocuments(user, category, null, query, pageable);

        // Кладем в модель сам объект Page, он содержит всю нужную информацию
        model.addAttribute("documentPage", documentPage);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("category", category);
        model.addAttribute("query", query);

        return "documents"; // Возвращает templates/documents.html
    }

    @PostMapping("/web/documents/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam String category,
                                   @RequestParam(required = false) Set<String> tags,
                                   @AuthenticationPrincipal User user,
                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пожалуйста, выберите файл для загрузки.");
            return "redirect:/web/documents";
        }
        try {
            documentService.uploadDocument(file, category, tags, user);
            redirectAttributes.addFlashAttribute("successMessage", "Файл '" + file.getOriginalFilename() + "' успешно загружен!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось загрузить файл: " + e.getMessage());
        }
        return "redirect:/web/documents";
    }

    @GetMapping("/web/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @AuthenticationPrincipal User user) {
        // ИСПОЛЬЗУЕМ НОВЫЙ DTO
        FileDownloadDto fileDto = documentService.downloadDocument(id, user);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileDto.fileName() + "\"")
                .contentType(fileDto.mediaType())
                .body(fileDto.resource());
    }

    @PostMapping("/web/documents/{id}/delete")
    public String deleteDocument(@PathVariable Long id,
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        try {
            documentService.deleteDocument(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Документ успешно удален.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/web/documents";
    }

    @GetMapping("web/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')") // Только пользователь с ролью ADMIN может получить доступ
    public String getAdminDashboard(Model model,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Set<String> tags,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(required = false) String username) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DocumentDto> documentPage = documentService.getAllDocumentsForAdmin(pageable, category, tags, query, username);

        model.addAttribute("documentPage", documentPage);
        model.addAttribute("category", category);
        model.addAttribute("tags", tags != null ? String.join(",", tags) : "");
        model.addAttribute("query", query);
        model.addAttribute("username", username);

        return "admin-dashboard";
    }
}
