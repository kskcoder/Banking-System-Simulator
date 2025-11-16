package com.tejas.transactionservice.feign;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class FeignInterceptor implements RequestInterceptor {

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		
        if (attributes != null) {
        	HttpServletRequest request = attributes.getRequest();
        	String authorization = request.getHeader("Authorization");
        	template.header("Authorization", authorization);
        }
	}

}
