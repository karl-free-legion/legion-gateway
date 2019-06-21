package com.zcs.legion.gateway.filter;

/**
 * 针对Token进行拦截
 * @author lance
 * 6/21/2019 18:03
 */
public abstract class AbstractTokenFilter implements GatewayFilter {
    /**
     * filter顺序
     *
     * @return 0
     */
    @Override
    public int order() {
        return 0;
    }

    /**
     * 是否启用
     *
     * @return false
     */
    @Override
    public boolean enable() {
        return false;
    }
}
