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
                .anyMatch(role -> role.equals("ADMIN") || role.equals("ROLE_ADMIN"));
        return isAdmin;
    }
    
    public static String getUserId() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth == null) {
    		return null;
    	}
        return auth.getName();
    }
    
    public static String getRole() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	
    	if (auth == null || auth.getAuthorities().isEmpty()) {
    		return null;
    	}
    	
    	String authority = auth.getAuthorities().stream()
    			.findFirst()
    			.map(GrantedAuthority::getAuthority)
    			.orElse(null);
    	
    	if (authority != null && authority.startsWith("ROLE_")) {
    		return authority.substring(5);
    	}
    	
    	return authority;
    }
    
	public static String hash(String data) {
	    return DigestUtils.sha256Hex(data);
	}
}
