package com.zcs.legion.gateway.config;

import com.alibaba.fastjson.JSONObject;
import com.zcs.legion.gateway.filter.AbstractTokenFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Comparator;
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

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private GroupTag groupTag;

    public GatewayWebMvcConfigurer(List<AbstractTokenFilter> filters) {
        this.filters = filters;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GatewayFilterBean());
    }

    class GatewayFilterBean extends HandlerInterceptorAdapter {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
            log.info("groupTag value:{} , url:{}" , JSONObject.toJSONString(groupTag), request.getRequestURI());

            boolean flag = false;
            if(!CollectionUtils.isEmpty(groupTag.getInterceptorPath())){
                flag = groupTag.getInterceptorPath().stream().anyMatch(p -> pathMatcher.match(p ,
                        request.getRequestURI()));
            }
            if(flag){
                filters.stream().filter(AbstractTokenFilter::enable)
                        .sorted(Comparator.comparing(AbstractTokenFilter::order))
                        .forEach(f -> f.handler(request));
            }
            return true;
        }
    }

}
