package com.kb.service;

import com.kb.model.Article;
import com.kb.model.ArticleHistory;
import com.kb.repository.ArticleHistoryRepository;
import com.kb.repository.ArticleRepository;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleHistoryRepository historyRepository;

    /**
     * Get all articles with pagination
     */
    public Page<Article> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    /**
     * Get a single article by ID and increment view count
     */
    @Transactional
    public Article getArticleById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + id));

        // Increment view count
        article.setViews(article.getViews() + 1);
        return articleRepository.save(article);
    }

    /**
     * Create a new article and its initial history record
     */
    @Transactional
    public Article createArticle(Article article) {
        // Validate article
        validateArticle(article);

        // Set initial values
        article.setViews(0);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        // Save the article
        Article savedArticle = articleRepository.save(article);

        // Create initial history entry
        createHistoryRecord(savedArticle);

        log.info("Created new article with ID: {}", savedArticle.getId());
        return savedArticle;
    }

    /**
     * Update an existing article and create a history record
     */
    @Transactional
    public Article updateArticle(Long id, Article articleDetails) {
        // Validate article
        validateArticle(articleDetails);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + id));

        // Create history record before updating
        createHistoryRecord(article);

        // Update article details
        article.setTitle(articleDetails.getTitle());
        article.setContent(articleDetails.getContent());
        article.setKeywords(articleDetails.getKeywords());
        article.setUpdatedAt(LocalDateTime.now());

        Article updatedArticle = articleRepository.save(article);
        log.info("Updated article with ID: {}", id);
        return updatedArticle;
    }

    /**
     * Delete an article and its history
     */
    @Transactional
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + id));

        // Delete all history records first
        historyRepository.deleteAll(historyRepository.findByArticleIdOrderByModifiedAtDesc(id));

        // Delete the article
        articleRepository.delete(article);
        log.info("Deleted article with ID: {}", id);
    }

    /**
     * Search articles with multiple criteria
     */
    public Page<Article> searchArticles(
            String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Integer minViews,
            String[] tags,
            Pageable pageable) {

        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search in title, content, and keywords
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("content")), likePattern),
                        cb.like(cb.lower(root.get("keywords")), likePattern)
                ));
            }

            // Date range filters
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            // Minimum views filter
            if (minViews != null && minViews > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("views"), minViews));
            }

            // Tags filter
            if (tags != null && tags.length > 0) {
                List<Predicate> tagPredicates = new ArrayList<>();
                for (String tag : tags) {
                    if (StringUtils.hasText(tag)) {
                        tagPredicates.add(cb.like(root.get("keywords"), "%" + tag.trim() + "%"));
                    }
                }
                if (!tagPredicates.isEmpty()) {
                    predicates.add(cb.or(tagPredicates.toArray(new Predicate[0])));
                }
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };

        return articleRepository.findAll(spec, pageable);
    }

    /**
     * Get article history
     */
    public List<ArticleHistory> getArticleHistory(Long articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new EntityNotFoundException("Article not found with id: " + articleId);
        }
        return historyRepository.findByArticleIdOrderByModifiedAtDesc(articleId);
    }

    /**
     * Export article to markdown format
     */
    public String exportToMarkdown(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + id));

        FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();
        StringBuilder markdown = new StringBuilder();

        // Add title
        markdown.append("# ").append(article.getTitle()).append("\n\n");

        // Add content
        markdown.append(converter.convert(article.getContent())).append("\n\n");

        // Add metadata
        markdown.append("---\n");
        markdown.append("Keywords: ").append(article.getKeywords()).append("\n");
        markdown.append("Created: ").append(article.getCreatedAt()).append("\n");
        markdown.append("Last Updated: ").append(article.getUpdatedAt()).append("\n");
        markdown.append("Views: ").append(article.getViews()).append("\n");

        return markdown.toString();
    }

    /**
     * Increment view count for an article
     */
    @Transactional
    public Article incrementViews(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found with id: " + id));
        article.setViews(article.getViews() + 1);
        return articleRepository.save(article);
    }

    /**
     * Create a history record for an article
     */
    private void createHistoryRecord(Article article) {
        ArticleHistory history = new ArticleHistory();
        history.setArticle(article);
        history.setContent(article.getContent());
        historyRepository.save(history);
    }

    /**
     * Validate article data
     */
    private void validateArticle(Article article) {
        if (!StringUtils.hasText(article.getTitle())) {
            throw new IllegalArgumentException("Article title cannot be empty");
        }
        if (!StringUtils.hasText(article.getContent())) {
            throw new IllegalArgumentException("Article content cannot be empty");
        }
    }

    /**
     * Get recent articles
     */
    public Page<Article> getRecentArticles(Pageable pageable) {
        return articleRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Get popular articles (by view count)
     */
    public Page<Article> getPopularArticles(Pageable pageable) {
        return articleRepository.findAllByOrderByViewsDesc(pageable);
    }

    /**
     * Get articles by keyword/tag
     */
    public Page<Article> getArticlesByKeyword(String keyword, Pageable pageable) {
        return articleRepository.findByKeywordsContainingIgnoreCase(keyword, pageable);
    }
}