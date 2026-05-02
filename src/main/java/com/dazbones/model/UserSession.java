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
        return "ログイン";
    }
}