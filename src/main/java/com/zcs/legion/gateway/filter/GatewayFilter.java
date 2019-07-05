package com.zcs.legion.gateway.filter;

import com.zcs.legion.gateway.filter.exception.InvalidTokenException;

import javax.servlet.http.HttpServletRequest;

/**
 * 网关统一拦截
 * @author lance
 * 6/21/2019 18:02
 */
public interface GatewayFilter {
    /**
     * filter顺序
     * @return 0
     */
    int order();

    /**
     * 是否启用
     * @return false
     */
    boolean enable();

    /**
     * 处理具体业务
     */
    void handler(HttpServletRequest request) throws InvalidTokenException;
}
