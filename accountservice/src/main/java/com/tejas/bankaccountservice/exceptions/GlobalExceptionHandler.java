package com.tejas.bankaccountservice.exceptions;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tejas.bankingcommon.dto.ApiError;
import com.tejas.bankingcommon.exceptions.*;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler
	public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.UNAUTHORIZED.value(),
				"INCORRECT_PASSWORD",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleForbidden(ForbiddenException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.FORBIDDEN.value(),
				"FORBIDDEN_ACCESS",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleAccountNotFound(NotFoundException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				"ACCOUNT_NOT_FOUND",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleGeneralException(GeneralServerException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"SERVER_ERROR",
				"Something went wrong",
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
	
	@ExceptionHandler
	public ResponseEntity<ApiError> handleNoContentException(NoContentException e, HttpServletRequest req) {
		ApiError error = new ApiError(
				LocalDateTime.now(),
				HttpStatus.NO_CONTENT.value(),
				"NO_CONTENT",
				e.getMessage(),
				req.getRequestURI()
		);
		
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(error);
	}	
}
