package com.tejas.bankpaymentservice.feign;

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
	@Value("${paymentInternalSecretKey}")
	private String paymentInternalSecretKey;
	
	@Value("${interServiceSecretKey}")
	private String interServiceSecretKey;

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		
        if (attributes != null) {
        	HttpServletRequest request = attributes.getRequest();
        	
        	String external = request.getHeader("X-External");
        	
        	if (external != null && external.equals("1")) {
        		// External payment - use paymentInternalSecretKey
        		template.header("X-Internal-Auth", paymentInternalSecretKey);
        	} else {
        		String authorization = request.getHeader("Authorization");
        		if (authorization != null) {
        			template.header("Authorization", authorization);
        		} else {
        			// No Authorization header available - use inter-service key
        			template.header("X-Internal-Auth", interServiceSecretKey);
        			template.header("X-User-Role", UserType.INTERNAL_SERVICE.toString());
        			template.header("X-User-Id", "INTERNAL_PAYMENT_SERVICE");
        		}
        	}
        } else {
        	// Called from Kafka listener or other non-HTTP context
        	template.header("X-Internal-Auth", interServiceSecretKey);
        	template.header("X-User-Role", UserType.INTERNAL_SERVICE.toString());
        	template.header("X-User-Id", "INTERNAL_PAYMENT_SERVICE");
        }
	}

}

