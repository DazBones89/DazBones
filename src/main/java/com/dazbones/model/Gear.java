package com.dazbones.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gear")
public class Gear {
    @Version private Long version;
    public Long getVersion(){return version;}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @jakarta.validation.constraints.NotBlank(message = "道具名を入力してください")
    @jakarta.validation.constraints.Size(max = 100, message = "道具名は100文字以内です")
    private String name;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(columnDefinition = "TEXT")
    @jakarta.validation.constraints.Size(max = 1000, message = "コメントは1000文字以内です")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void preInsert() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // getter/setter
    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getName() { return name; }
    public Long getOwnerId() { return ownerId; }
    public String getComment() { return comment; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public void setComment(String comment) { this.comment = comment; }
}
