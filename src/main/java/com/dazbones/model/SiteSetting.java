package com.dazbones.model;
import jakarta.persistence.*;
@Entity @Table(name="site_settings")
public class SiteSetting {
 @Id @Column(name="setting_key") public String key;
 @Column(name="setting_value",nullable=false) public String value;
 public SiteSetting(){} public SiteSetting(String key,String value){this.key=key;this.value=value;}
}
