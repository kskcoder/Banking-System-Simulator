package com.tejas.bankpaymentservice.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class FeignInterceptor implements RequestInterceptor {
	@Value("${paymentInternalSecretKey}")
	private String paymentInternalSecretKey;

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		
        if (attributes != null) {
        	HttpServletRequest request = attributes.getRequest();
        	
        	String external = request.getHeader("X-External");
        	
        	if (external.equals("1")) {
        		template.header("X-Internal-Auth", paymentInternalSecretKey);
        	} else {
        		String authorization = request.getHeader("Authorization");
        		template.header("Authorization", authorization);
        	}
        }
	}

}

