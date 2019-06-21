package com.zcs.legion.gateway.config;

import com.google.common.collect.Lists;
import com.zcs.legion.gateway.filter.AbstractIpFilter;
import com.zcs.legion.gateway.filter.AbstractTokenFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;

/**
 * GatewayConfigurer
 * @author lance
 * 6/21/2019 17:23
 */
@Configuration
public class GatewayWebMvcConfigurer implements WebMvcConfigurer {
    public final List<AbstractTokenFilter> filters;
    public final List<AbstractIpFilter> ipFilters;

    public GatewayWebMvcConfigurer(List<AbstractTokenFilter> filters, List<AbstractIpFilter> ipFilters){
        this.filters = filters;
        this.ipFilters = ipFilters;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GatewayFilterBean()).addPathPatterns("/localApps/**", "/ops/**");
        registry.addInterceptor(new GatewayIpFilterBean()).addPathPatterns("/tsm/**");
    }

    class GatewayFilterBean extends HandlerInterceptorAdapter {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            filters.stream().filter(AbstractTokenFilter::enable)
                    .sorted(Comparator.comparing(AbstractTokenFilter::order))
                    .forEach(f -> f.handler(request));
            return true;
        }
    }

    class GatewayIpFilterBean extends HandlerInterceptorAdapter {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            ipFilters.stream().filter(AbstractIpFilter::enable)
                    .sorted(Comparator.comparing(AbstractIpFilter::order))
                    .forEach(f -> f.handler(request));
            return true;
        }
    }
}
