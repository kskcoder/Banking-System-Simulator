package com.tejas.bankingcommon.exceptions;

public class BadRequestException extends RuntimeException {
	public BadRequestException(String msg) {
		super(msg);
	}
}