package com.zcs.legion.gateway.web;

import com.google.common.collect.Maps;
import com.zcs.legion.gateway.filter.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 
 * @author lance
 * 6/22/2019 13:57
 */
@Slf4j
@RestControllerAdvice
public class GlobalManageExceptionResolver {

	/**
	 * 记录全局异常
	 * @param em 异常对象
	 * @return
	 */
    @ExceptionHandler(Exception.class)
    public String ExceptionHandler(Exception em){
		log.warn("Handler Exception:", em);
        return "error";
    }

	/**
	 * InvalidTokenException
	 * @param ex InvalidTokenException
	 * @return  ResponseEntity
	 */
	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity invalidToken(InvalidTokenException ex){
		Map<String, Object> result = Maps.newHashMap();
		result.put("code", ex.getCode());
		result.put("message", ex.getMessage());

		return ResponseEntity
				.status(HttpStatus.OK)
				.body(result);
	}
}
