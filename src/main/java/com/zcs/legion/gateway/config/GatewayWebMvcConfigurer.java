package com.zcs.legion.gateway.config;

import com.zcs.legion.common.ConstantsValues;
import com.zcs.legion.gateway.filter.AbstractTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GatewayConfigurer
 *
 * @author lance
 * 6/21/2019 17:23
 */
@Configuration
@AutoConfigureAfter(GroupTag.class)
public class GatewayWebMvcConfigurer implements WebMvcConfigurer {
    public final List<AbstractTokenFilter> filters;

    public GatewayWebMvcConfigurer(List<AbstractTokenFilter> filters) {
        this.filters = filters;
    }

    @Autowired
    private GroupTag groupTag;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if(!CollectionUtils.isEmpty(groupTag.getTokenTags().keySet())){
            registry.addInterceptor(new GatewayFilterBean()).excludePathPatterns(ConstantsValues.X_NOT_FILTER_PATH)
                    .addPathPatterns(Arrays.stream(groupTag.getTokenTags().keySet().toArray()).map(g -> "/" + g +"/**").collect(Collectors.toList()));
        }
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

}
