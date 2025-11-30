package com.tejas.bankpaymentservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tejas.bankingcommon.dto.CardVerificationRequest;
import com.tejas.bankingcommon.dto.CardVerificationResponse;

@FeignClient("bankcardservice")
public interface CardInterface {
	@PostMapping("card/verify")
	public ResponseEntity<CardVerificationResponse> verifyCard(@RequestBody CardVerificationRequest request);
}
