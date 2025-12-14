package com.tejas.accountservice.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuthUtils {
	public static String getRole() {
		return ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
	}
}
