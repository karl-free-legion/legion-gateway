package com.zcs.legion.gateway.config;

import com.zcs.legion.gateway.common.CacheUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限拦截
 *
 * @author lance
 * 1/8/2020 18:16
 */
class AccessHandlerInterceptor extends HandlerInterceptorAdapter {
    private CacheUtils cacheUtils;

    AccessHandlerInterceptor(CacheUtils cacheUtils) {
        this.cacheUtils = cacheUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return cacheUtils.hasKey("zcs:commons:access:gw");
    }
}
