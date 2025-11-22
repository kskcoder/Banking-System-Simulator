package com.tejas.accountservice.utils;

import org.springframework.stereotype.Component;

import com.tejas.accountservice.enums.AccountType;

@Component
public class AccountRules {
	public double getMinimumBalance(AccountType type) {
		return type == AccountType.SAVINGS ? 10000.0 : -50000.0;
	}
	
	public double getInterest(AccountType type, boolean aboveLakh) {
		return type == AccountType.SAVINGS ? aboveLakh ? 7.0 : 3.5 : 0.0;
	}
	
    public double getOverdraftLimit(AccountType type) {
    	return type == AccountType.CURRENT ? 50000 : 0;
    }
}
