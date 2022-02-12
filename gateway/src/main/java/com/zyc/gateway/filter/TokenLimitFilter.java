package com.zyc.gateway.filter;

import com.zyc.gateway.util.TokenTong;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局过滤器：implements GlobalFilter
 */
@Component
public class TokenLimitFilter implements GatewayFilter {
    private Map<String, TokenTong> tongMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //令牌桶限流
        //获取当前请求的url
        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getPath().value();
        System.out.println("当前的请求路径：" + requestPath);
        TokenTong tokenTong = tongMap.computeIfAbsent(requestPath, (s) -> {
            return new TokenTong(s, 5, 2);
        });
        //领取令牌
        boolean tokensNow = tokenTong.getTokensNow(2);
        if (tokensNow) {
            //获取到令牌，放行
            return chain.filter(exchange);
        }
        //没有拿到令牌，返回服务器繁忙
        String res = "服务器繁忙";
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        headers.put("Content-Type", Arrays.asList("application/json;charset=utf-8"));
        headers.put("Access-Control-Allow-Origin", Arrays.asList("*"));
        DataBufferFactory dataBufferFactory = response.bufferFactory();
        DataBuffer wrap = null;
        try {
            wrap = wrap = dataBufferFactory.wrap(res.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Mono<Void> voidMono = response.writeWith(Mono.just(wrap));
        return voidMono;
    }
}
