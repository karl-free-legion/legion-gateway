package com.zcs.legion.gateway.config;

import com.zcs.legion.gateway.common.CacheHelper;
import com.zcs.legion.gateway.utils.DateUtils;
import com.zcs.legion.gateway.utils.DecryptUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * 权限拦截
 *
 * @author lance
 * 1/8/2020 18:16
 */
class AccessHandlerInterceptor extends HandlerInterceptorAdapter {
    private CacheHelper cacheHelper;

    AccessHandlerInterceptor(CacheHelper cacheHelper) {
        this.cacheHelper = cacheHelper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return checkAccess();
    }

    /**
     * 校验权限
     */
    private boolean checkAccess() {
        if (cacheHelper.hasAccessKey(CacheHelper.DEFAULT_KEY)) {
            return true;
        }

        Map<Object, Object> entries = cacheHelper.getMapForHash(CacheHelper.DEFAULT_KEY);
        if (Objects.isNull(entries) || entries.isEmpty()) {
            return false;
        }

        String expireAt = null;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String key = entry.getKey() + "";
            String val = entry.getValue() + "";
            expireAt = DecryptUtils.decrypt(val, CacheHelper.DEFAULT_KEY, key.getBytes(StandardCharsets.UTF_8));
        }

        if (StringUtils.isBlank(expireAt) || !DateUtils.compare(expireAt)) {
            return false;
        }

        cacheHelper.setAccessKey(CacheHelper.DEFAULT_KEY, "hello world");
        return true;
    }
}
