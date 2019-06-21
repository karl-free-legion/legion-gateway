package com.zcs.legion.gateway.filter;

import com.zcs.legion.gateway.filter.exception.InvalidTokenException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 
 * @author lance
 * 6/21/2019 17:22
 */
@Component
public class GroupTokenFilter extends AbstractTokenFilter {
    @Override
    public boolean enable() {
        return true;
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public void handler(HttpServletRequest request) throws InvalidTokenException {

    }
}
