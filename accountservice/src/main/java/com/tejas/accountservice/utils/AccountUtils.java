package com.tejas.accountservice.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AccountUtils {
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null)
            return false;

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        return isAdmin;
    }
    
    public static String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    
}
