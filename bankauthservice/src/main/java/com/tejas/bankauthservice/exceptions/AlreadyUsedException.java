package com.tejas.bankauthservice.exceptions;

public class AlreadyUsedException extends RuntimeException{
	public AlreadyUsedException(String msg) {
		super(msg);
	}
}
