package com.dazbones.model;

import java.io.Serializable;

public class UserSession implements Serializable {

    private final String role;
    private final String loginId;
    private final Long memberId;
    private final long generation;
    private final Long credentialVersion;

    public UserSession(String role, String loginId, Long memberId, long generation, Long credentialVersion) {
        this.role=role; this.loginId=loginId; this.memberId=memberId;
        this.generation=generation; this.credentialVersion=credentialVersion;
    }
    public String getLoginId(){return loginId;}
    public Long getMemberId(){return memberId;}
    public long getGeneration(){return generation;}
    public Long getCredentialVersion(){return credentialVersion;}


    public UserSession(String role) {
        this(role, null, null, 0, null);
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isEditor() {
        return "editor".equals(role);
    }

    public boolean isLoggedIn() {
        return role != null && !role.isBlank();
    }

    public boolean canManage() {
        return isAdmin() || isEditor();
    }

    public String getDisplayName() {
        if (isAdmin()) {
            return "管理者";
        }
        if (isEditor()) {
            return "ログイン中";
        }
        return "member".equals(role) ? "メンバー" : "ログイン";
    }
}