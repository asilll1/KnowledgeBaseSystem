package com.kb.controller;

import com.kb.model.Article;
import com.kb.model.ArticleHistory;
import com.kb.service.ArticleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArticleController {
    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<Page<Article>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return ResponseEntity.ok(articleService.getAllArticles(PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticle(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(articleService.getArticleById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Article> createArticle(@Valid @RequestBody Article article) {
        try {
            Article createdArticle = articleService.createArticle(article);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdArticle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Article> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody Article article) {
        try {
            Article updatedArticle = articleService.updateArticle(id, article);
            return ResponseEntity.ok(updatedArticle);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id) {
        try {
            articleService.deleteArticle(id);
            return ResponseEntity.ok()
                    .body(Map.of("message", "Article successfully deleted", "id", id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete article", "message", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Article>> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Integer minViews,
            @RequestParam(required = false) String[] tags) {

        return ResponseEntity.ok(articleService.searchArticles(
                keyword, fromDate, toDate, minViews, tags, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ArticleHistory>> getArticleHistory(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(articleService.getArticleHistory(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<String> exportToMarkdown(@PathVariable Long id) {
        try {
            String markdown = articleService.exportToMarkdown(id);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/markdown")
                    .header("Content-Disposition", "attachment; filename=\"article-" + id + ".md\"")
                    .body(markdown);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}