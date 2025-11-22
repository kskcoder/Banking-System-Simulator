package com.tejas.bankingcommon.exceptions;

public class InvalidRequestException extends RuntimeException {
	public InvalidRequestException(String msg) {
		super(msg);
	}
}
