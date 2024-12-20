package com.kb.repository;

import com.kb.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {
    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Article> findAllByOrderByViewsDesc(Pageable pageable);
    Page<Article> findByKeywordsContainingIgnoreCase(String keyword, Pageable pageable);
}