package com.zcs.legion.gateway.filter.exception;

import lombok.Data;

/**
 * InvalidTokenException
 * @author lance
 * 6/21/2019 17:59
 */
@Data
public class InvalidTokenException extends RuntimeException{
    private int code;
    private String message;

    public InvalidTokenException(int code, String message){
        this.code = code;
        this.message = message;
    }

    public static final InvalidTokenException SE_DEC_TOKEN_EXCEPTION = new InvalidTokenException(3000, "token解密失败");

    public static final InvalidTokenException TAG_CHECKE_ILLEGAL = new InvalidTokenException(3001, "请求tag配置文件中不存在");

    public static final InvalidTokenException TOKEN_CHECK_EMPTY = new InvalidTokenException(3002 , "token不可为空！");

    public static final InvalidTokenException TOKEN_EXPIRE_OUT = new InvalidTokenException(2000, "登录超时");

}
