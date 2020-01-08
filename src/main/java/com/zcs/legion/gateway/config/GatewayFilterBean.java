package com.zcs.legion.gateway.config;

import com.zcs.legion.gateway.filter.AbstractTokenFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;

class GatewayFilterBean extends HandlerInterceptorAdapter {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private GroupTag groupTag;
    private List<AbstractTokenFilter> filters;

    GatewayFilterBean(GroupTag groupTag, List<AbstractTokenFilter> filters) {
        this.filters = filters;
        this.groupTag = groupTag;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        boolean flag = false;
        if (!CollectionUtils.isEmpty(groupTag.getInterceptorPath())) {
            flag = groupTag.getInterceptorPath().stream().anyMatch(p -> pathMatcher.match(p,
                    request.getRequestURI()));
        }
        if (flag) {
            filters.stream().filter(AbstractTokenFilter::enable)
                    .sorted(Comparator.comparing(AbstractTokenFilter::order))
                    .forEach(f -> f.handler(request));
        }
        return true;
    }
}
