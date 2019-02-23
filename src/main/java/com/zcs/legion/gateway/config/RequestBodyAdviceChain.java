package com.zcs.legion.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 
 * @author lance
 * @since 2019.2.23 16:58
 */
@Slf4j
@ControllerAdvice
public class RequestBodyAdviceChain extends RequestBodyAdviceAdapter {
    @Override
    public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> clazz) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage message, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        HttpInputMessage input = super.beforeBodyRead(message, parameter, targetType, converterType);
        return input;
    }
}
