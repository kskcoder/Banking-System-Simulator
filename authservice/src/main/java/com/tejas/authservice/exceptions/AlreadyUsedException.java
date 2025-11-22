package com.tejas.authservice.exceptions;

public class AlreadyUsedException extends RuntimeException{
	public AlreadyUsedException(String msg) {
		super(msg);
	}
}
