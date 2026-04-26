package com.dazbones.model;

import java.io.Serializable;

public class UserSession implements Serializable {

    private final String role;

    public UserSession(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    // ===== 権限判定 =====

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isEditor() {
        return "editor".equals(role);
    }

    public boolean isLoggedIn() {
        return role != null;
    }

    public boolean canManage() {
        return isAdmin() || isEditor();
    }

    // ===== 表示用 =====

    public String getDisplayName() {
        if (isAdmin()) {
            return "管理者";
        }
        if (isEditor()) {
            return "ログイン中";
        }
        return "ログイン";
    }
}