package com.zcs.legion.gateway.config;

/**
 * GatewayConfigurer
 *
 * @author lance
 * 6/21/2019 17:23
 */
/*
@Configuration
public class GatewayWebMvcConfigurer implements WebMvcConfigurer {
    public final List<AbstractTokenFilter> filters;
    public final List<AbstractIpFilter> ipFilters;

    public GatewayWebMvcConfigurer(List<AbstractTokenFilter> filters, List<AbstractIpFilter> ipFilters) {
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
*/
