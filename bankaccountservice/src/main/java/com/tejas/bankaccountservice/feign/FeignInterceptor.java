package com.tejas.bankaccountservice.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.enums.InternalServiceType;
import com.tejas.bankingcommon.enums.UserType;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class FeignInterceptor implements RequestInterceptor {
	
	@Value("${interServiceSecretKey}")
	private String interServiceSecretKey;

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		
        if (attributes != null) {
        	HttpServletRequest request = attributes.getRequest();
        	String authorization = request.getHeader("Authorization");
        	if (authorization != null) {
        		template.header("Authorization", authorization);
        	} else {
        		template.header("X-Internal-Auth", interServiceSecretKey);
        		template.header("X-User-Role", UserType.INTERNAL_SERVICE.toString());
        		template.header("X-User-Id", InternalServiceType.ACCOUNT.toString());
        	}
        } else {
        	template.header("X-Internal-Auth", interServiceSecretKey);
        	template.header("X-User-Role", UserType.INTERNAL_SERVICE.toString());
        	template.header("X-User-Id", InternalServiceType.ACCOUNT.toString());
        }
	}

}
