package com.tejas.transactionservice.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("account-service")
public interface AccountInterface {
	@GetMapping("accounts/accountnumber/{accountNumber}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccountNumber(@PathVariable String accountNumber);
	
	@GetMapping("accounts/accountIds")
	public ResponseEntity<List<Long>> getAccountIdsByUserId();
}
