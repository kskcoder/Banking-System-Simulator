package com.tejas.bankaccountservice.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {
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
}
