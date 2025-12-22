package com.tejas.bankaccountservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tejas.bankingcommon.dto.ContactDetails;

import jakarta.validation.Valid;

@FeignClient("auth-service")
public interface AuthInterface {
	@GetMapping("auth/getcontact/{userId}")
	public abstract ResponseEntity<ContactDetails> getContact(@Valid @PathVariable Long userId);
}
