package com.tejas.banktransactionservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("bankaccountservice")
public interface AccountInterface {
	@GetMapping("accounts/accountnumber/{accountNumber}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccountNumber(@PathVariable String accountNumber);
}
