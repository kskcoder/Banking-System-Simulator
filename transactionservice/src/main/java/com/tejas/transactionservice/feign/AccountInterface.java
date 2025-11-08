package com.tejas.transactionservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tejas.transactionservice.models.TransferRequest;

@FeignClient("account-service")
public interface AccountInterface {
	@PostMapping("accounts/debit")
	public ResponseEntity<String> debitAccount(@RequestBody TransferRequest request);
	
	@PostMapping("accounts/credit")
	public ResponseEntity<String> creditAccount(@RequestBody TransferRequest request);
}
