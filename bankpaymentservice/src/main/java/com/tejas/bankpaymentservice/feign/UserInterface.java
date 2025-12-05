package com.tejas.bankpaymentservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tejas.bankingcommon.dto.OtpRequestDTO;

import jakarta.validation.Valid;

@FeignClient("auth-service")
public interface UserInterface {
	@GetMapping("auth/sendotp/{userId}")
	public ResponseEntity<Boolean> sendPaymentOtp(@Valid @RequestBody OtpRequestDTO request); 
}
