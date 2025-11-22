package com.tejas.bankingcommon.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ApiError {
	private LocalDateTime timetamp;
	private int status;
	private String error;
	private String message;
	private String path;
}
