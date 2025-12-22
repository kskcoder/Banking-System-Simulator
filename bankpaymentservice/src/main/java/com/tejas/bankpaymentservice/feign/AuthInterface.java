package com.tejas.bankpaymentservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tejas.bankingcommon.dto.OtpRequestDTO;
import com.tejas.bankingcommon.dto.OtpValidateRequest;
import com.tejas.bankingcommon.dto.OtpValidateResponse;

import jakarta.validation.Valid;

@FeignClient("bankauthservice")
public interface AuthInterface {
	@GetMapping("auth/sendotp/{userId}")
	public ResponseEntity<Boolean> sendPaymentOtp(@Valid @RequestBody OtpRequestDTO request); 
	
	@GetMapping("auth/submitotp")
	public ResponseEntity<OtpValidateResponse> validateOtp(@Valid @RequestBody OtpValidateRequest request); 
}
