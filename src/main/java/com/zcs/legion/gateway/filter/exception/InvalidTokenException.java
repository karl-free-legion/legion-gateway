package com.zcs.legion.gateway.filter.exception;

import lombok.Data;

/**
 * InvalidTokenException
 * @author lance
 * 6/21/2019 17:59
 */
@Data
public class InvalidTokenException extends RuntimeException{
    private String code;
    private String message;

    public InvalidTokenException(String code, String message){
        this.code = code;
        this.message = message;
    }
}
