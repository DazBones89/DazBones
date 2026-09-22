package com.dazbones.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="instagram_posts")
public class InstagramPost {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=64) private String shortcode;
 @Column(nullable=false,length=255) private String url;
 @Column(name="posted_at",nullable=false) private LocalDateTime postedAt;
 @Column(name="display_order") private Integer displayOrder;
 public Long getId(){return id;}
 public String getShortcode(){return shortcode;} public void setShortcode(String v){shortcode=v;}
 public String getUrl(){return url;} public void setUrl(String v){url=v;}
 public LocalDateTime getPostedAt(){return postedAt;} public void setPostedAt(LocalDateTime v){postedAt=v;}
 public Integer getDisplayOrder(){return displayOrder;} public void setDisplayOrder(Integer v){displayOrder=v;}
}
