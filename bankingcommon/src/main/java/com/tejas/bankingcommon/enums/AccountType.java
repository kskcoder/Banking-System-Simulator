package com.tejas.bankingcommon.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum AccountType {
	SAVINGS,
	CURRENT;
	
	@JsonCreator
	public static AccountType fromValue(String value) {
        return AccountType.valueOf(value.toUpperCase());
    }
}
