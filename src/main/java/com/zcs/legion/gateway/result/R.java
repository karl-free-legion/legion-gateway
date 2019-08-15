package com.zcs.legion.gateway.result;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 定义返回
 *
 * @author lance
 * @since 2019.2.17 18:43
 */
public class R extends HashMap<String, Object> {
    private final static long serialVersionUID = 1L;
    private final static String CODE = "code";
    private final static String ERROR_CODE = "errorCode";
    private final static String MESSAGE = "message";

    public R() {
        put(CODE, 0);
        put(MESSAGE, "success");
    }

    public static R error() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "未知异常，请联系管理员");
    }

    public static R error(String msg) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    public static R error(int code, String message) {
        return error(code, code + "", message);
    }

    public static R error(int code, String errorCode, String message) {
        R r = new R();
        r.put(CODE, code);
        r.put(ERROR_CODE, errorCode);
        r.put(MESSAGE, message);
        return r;
    }

    public static R success(Object message) {
        R r = new R();
        r.put(MESSAGE, message);
        return r;
    }

    public static R success(int code, Object message) {
        R r = new R();
        r.put(CODE, code);
        r.put(ERROR_CODE, code);
        r.put(MESSAGE, message);
        return r;
    }

    public static R success(String errorCode, String message) {
        R r = new R();
        r.put(ERROR_CODE, errorCode);
        r.put(MESSAGE, message);
        return r;
    }

    public static R success(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R success() {
        return new R();
    }

    @Override
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
