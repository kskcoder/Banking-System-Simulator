package com.tejas.authservice.exceptions;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tejas.bankingcommon.dto.ApiError;
import com.tejas.bankingcommon.exceptions.BadRequestException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NotFoundException;
import com.tejas.bankingcommon.exceptions.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleAlreadyUsed(AlreadyUsedException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.CONFLICT.value(),
				"ALREADY_USED",
				e.getMessage() +" is already used.",
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.UNAUTHORIZED.value(),
				"UNAUTHORIZED_USER",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleUserNotFound(NotFoundException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				"USER_NOT_FOUND",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.BAD_REQUEST.value(),
				"BAD_REQUEST",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleGeneralGeneration(GeneralServerException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"SERVER_ERROR",
				"Something went wrong",
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
