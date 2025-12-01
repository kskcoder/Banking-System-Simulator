package com.tejas.bankpaymentservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tejas.bankingcommon.dto.Transaction;
import com.tejas.bankingcommon.dto.TransferRequest;

import jakarta.validation.Valid;

@FeignClient("transaction-service")
public interface TransactionInterface {
	
	@PostMapping("transactions/transfer")
	public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request); 
}
