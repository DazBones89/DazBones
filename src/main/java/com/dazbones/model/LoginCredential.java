package com.dazbones.model;
import jakarta.persistence.*;
@Entity @Table(name="login_credentials")
public class LoginCredential {
    @Id @Column(name="login_id", length=64) private String loginId;
    @Column(name="code_hash", nullable=false, length=100) private String codeHash;
    @Column(nullable=false, length=16) private String role;
    @Column(name="member_id") private Long memberId;
    @Version private Long version;
    public String getLoginId(){return loginId;}
    public void setLoginId(String v){loginId=v;}
    public String getCodeHash(){return codeHash;}
    public void setCodeHash(String v){codeHash=v;}
    public String getRole(){return role;}
    public void setRole(String v){role=v;}
    public Long getMemberId(){return memberId;}
    public void setMemberId(Long v){memberId=v;}
    public Long getVersion(){return version;}
}
