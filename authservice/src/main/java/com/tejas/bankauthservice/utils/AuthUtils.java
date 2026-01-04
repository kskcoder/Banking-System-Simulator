package com.tejas.bankauthservice.utils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {
    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null)
            return false;
        
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ADMIN"));
        return isAdmin;
    }
    
    public static String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    
	public static String hash(String data) {
	    return DigestUtils.sha256Hex(data);
	}
}
