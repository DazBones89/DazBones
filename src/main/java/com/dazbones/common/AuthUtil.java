package com.dazbones.common;

import com.dazbones.model.UserSession;
import jakarta.servlet.http.HttpSession;

public class AuthUtil {

    private AuthUtil() {
    }

    public static UserSession getUserSession(HttpSession session) {
        return (UserSession) session.getAttribute("userSession");
    }

    public static boolean isLoggedIn(HttpSession session) {
        UserSession userSession = getUserSession(session);
        return userSession != null && userSession.isLoggedIn();
    }

    public static boolean isAdmin(HttpSession session) {
        UserSession userSession = getUserSession(session);
        return userSession != null && userSession.isAdmin();
    }

    public static boolean canManage(HttpSession session) {
        UserSession userSession = getUserSession(session);
        return userSession != null && userSession.canManage();
    }

    public static String denyAdmin(HttpSession session) {
        if (!isLoggedIn(session)) {
            return "error/404";
        }

        if (!isAdmin(session)) {
            return "error/403";
        }

        return null;
    }

    public static String denyManage(HttpSession session) {
        if (!isLoggedIn(session)) {
            return "error/404";
        }

        if (!canManage(session)) {
            return "error/403";
        }

        return null;
    }
}