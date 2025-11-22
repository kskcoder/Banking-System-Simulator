package com.tejas.bankcardservice.exceptions;

public class AlreadyExistsException extends RuntimeException{
	public AlreadyExistsException(String msg) {
		super(msg);
	}
}
