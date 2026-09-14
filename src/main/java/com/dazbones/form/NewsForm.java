package com.dazbones.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class NewsForm {

    private Long id;
    @jakarta.validation.constraints.Pattern(regexp="PUBLIC|MEMBERS")
    @NotBlank
    private String audience = "PUBLIC";
    public String getAudience(){return audience;}
    public void setAudience(String value){audience=value;}

    @NotBlank(message = "タイトルは必須です")
    @jakarta.validation.constraints.Size(max=255)
    private String title;

    @NotBlank(message = "本文は必須です")
    @jakarta.validation.constraints.Size(max=10000)
    private String content;

    @NotNull(message = "公開日時は必須です")
    private LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
