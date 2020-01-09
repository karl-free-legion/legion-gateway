package com.zcs.legion.gateway.config;

import com.zcs.legion.gateway.common.CacheHelper;
import com.zcs.legion.gateway.common.ConstantsValues;
import com.zcs.legion.gateway.filter.AbstractTokenFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * GatewayConfigurer
 *
 * @author lance
 * 6/21/2019 17:23
 */
@Slf4j
@Configuration
public class GatewayWebMvcConfigurer implements WebMvcConfigurer {
    public final List<AbstractTokenFilter> filters;
    @Autowired
    private CacheHelper cacheHelper;
    @Autowired
    private AccessProperties accessProperties;

    @Autowired
    private GroupTag groupTag;

    public GatewayWebMvcConfigurer(List<AbstractTokenFilter> filters) {
        this.filters = filters;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (accessProperties.isEnable()) {
            registry.addInterceptor(new AccessHandlerInterceptor(cacheHelper));
        }
        registry.addInterceptor(new GatewayFilterBean(groupTag, filters)).excludePathPatterns(ConstantsValues.X_NOT_FILTER_PATH);
    }
}
