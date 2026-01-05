package com.tejas.bankaccountservice.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.enums.UserType;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class FeignInterceptor implements RequestInterceptor {
	
	@Value("${accountSecretKey}")
	private String accountSecretKey;

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		
        if (attributes != null) {
        	HttpServletRequest request = attributes.getRequest();
        	String authorization = request.getHeader("Authorization");
        	if (authorization != null) {
        		template.header("Authorization", authorization);
        	}
        } else {
        	// Called from Kafka listener or other non-HTTP context
        	template.header("X-Internal-Auth", accountSecretKey);
        	template.header("X-User-Role", UserType.INTERNAL_SERVICE.toString());
        	template.header("X-User-Id", "INTERNAL_ACCOUNT_SERVICE");
        }
	}

}
