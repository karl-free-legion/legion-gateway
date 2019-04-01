package com.zcs.legion.gateway.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GlobalManageExceptionResolver {

	/**
	 * 记录全局异常
	 * @param em 异常对象
	 * @return
	 */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String ExceptionHandler(Exception em){
		log.error("Handler Exception:", em);
        return "error";
    }
}
