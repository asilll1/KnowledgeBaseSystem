package com.kb.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "article_history")
public class ArticleHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        modifiedAt = LocalDateTime.now();
    }
}