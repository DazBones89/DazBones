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
}