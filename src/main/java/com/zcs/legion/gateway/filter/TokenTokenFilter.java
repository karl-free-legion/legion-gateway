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
public class TokenTokenFilter extends AbstractTokenFilter {

    @Override
    public boolean enable() {
        return true;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public void handler(HttpServletRequest request) throws InvalidTokenException {
        ///
        throw new InvalidTokenException("333", "44444");
    }
}
