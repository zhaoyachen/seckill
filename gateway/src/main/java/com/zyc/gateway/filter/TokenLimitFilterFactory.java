package com.zyc.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class TokenLimitFilterFactory extends AbstractGatewayFilterFactory {

    @Autowired
    private TokenLimitFilter tokenLimitFilter;

    @Override
    public GatewayFilter apply(Object config) {
        return tokenLimitFilter;
    }

    @Override
    public String name() {
        return "TokenLimiter";
    }
}
