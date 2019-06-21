package com.zcs.legion.gateway.config;

import com.google.common.collect.Maps;
import com.zcs.legion.gateway.filter.exception.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 拦截token验证
 * @author lance
 * 6/21/2019 17:56
 */
@RestControllerAdvice
public class GatewayExceptionAdvice {

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
