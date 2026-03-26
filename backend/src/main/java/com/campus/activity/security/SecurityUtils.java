package com.campus.activity.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser == null ? null : loginUser.getUser().getId();
    }

    public static boolean hasAuthority(String authority) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || loginUser.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority ga : loginUser.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
