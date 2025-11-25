package com.tejas.bankcardservice.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tejas.bankingcommon.enums.AccountType;

@FeignClient("account-service")
public interface AccountInterface {
	@GetMapping("accounts/accountid/{accountId}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccountId(@PathVariable long accountId);
	
	@GetMapping("accounts/accountid/{accountId}/type")
	public ResponseEntity<AccountType> getAccountTypeByAccountId(@PathVariable long accountId);
	
	@GetMapping("accounts/accountIds")
	public ResponseEntity<List<Long>> getAccountIdsByUserId();
}
