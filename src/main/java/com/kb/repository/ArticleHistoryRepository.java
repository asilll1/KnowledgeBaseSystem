package com.kb.repository;

import com.kb.model.ArticleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleHistoryRepository extends JpaRepository<ArticleHistory, Long> {
    List<ArticleHistory> findByArticleIdOrderByModifiedAtDesc(Long articleId);
}