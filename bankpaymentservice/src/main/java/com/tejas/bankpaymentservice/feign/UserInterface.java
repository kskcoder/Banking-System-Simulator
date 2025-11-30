package com.tejas.bankpaymentservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tejas.bankingcommon.dto.UserContactDetails;

@FeignClient("auth-service")
public interface UserInterface {
	@GetMapping("auth/{userId}/contact")
	public ResponseEntity<UserContactDetails> getUserDetailsByUserId(@PathVariable long userId);
}
