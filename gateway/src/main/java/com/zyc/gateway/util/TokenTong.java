package com.zyc.gateway.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 令牌桶
 */
public class TokenTong {

    /**
     * 当前令牌桶的key
     */
    private String key;
    /**
     * 令牌桶的最大数量
     */
    private int maxTokens;

    /**
     * 每秒产生令牌的个数
     */
    private int secTokens;

    private StringRedisTemplate stringRedisTemplate;

    private String initTokenLua = "--判断key是否存在，如果不存在就初始化令牌桶\n" +
            "--获得参数key\n" +
            "local key = 'tongKey_'..KEYS[1]\n" +
            "--令牌桶的最大容量\n" +
            "local maxTokens = tonumber(ARGV[1])\n" +
            "--每秒产生的令牌数量\n" +
            "local secTokens = tonumber(ARGV[2])\n" +
            "--计算当前时间的微秒值\n" +
            "local nextTime = tonumber(ARGV[3])\n" +
            "--判断令牌桶是否存在\n" +
            "local result = redis.call('exists',key)\n" +
            "if result == 0 then\n" +
            "  --当前key不存在,进行初始化\n" +
            "  redis.call('hmset',key,'hasTokens',maxTokens,'maxTokens',maxTokens,'nextTime',nextTime,'secTokens',secTokens)\n" +
            "  return 1\n" +
            "end\n" +
            "return 0";

    private String getTokenLua = "--当前领取的令牌桶的key\n" +
            "local key = 'tongKey_'..KEYS[1]\n" +
            "--获取当前需要领取的令牌的数量\n" +
            "local getTokens = tonumber(ARGV[1])\n" +
            "--获取令牌桶的当前令牌数量\n" +
            "local hasTokens = tonumber(redis.call('hget',key,'hasTokens'))\n" +
            "--获取最大的令牌数\n" +
            "local maxTokens = tonumber(redis.call('hget',key,'maxTokens'))\n" +
            "--获取每秒生成令牌的数量\n" +
            "local secTokens = tonumber(redis.call('hget',key,'secTokens'))\n" +
            "--下一次可以生成令牌的时间\n" +
            "local nextTime = tonumber(redis.call('hget',key,'nextTime'))\n" +
            "--获取当前的时间(微秒值)\n" +
            "local nowArray = redis.call('time')\n" +
            "local nowTime = nowArray[1] * 1000000 + nowArray[2]\n" +
            "--单个令牌生成的耗时\n" +
            "local singleTokenTime = 1000000/secTokens\n" +
            "--获取超时时间\n" +
            "local timeout = tonumber(ARGV[2] or -1)\n" +
            "--判断超时时间\n" +
            "if timeout ~= -1 then\n" +
            "  --有超时时间\n" +
            "  if timeout < nextTime - nowTime then\n" +
            "    --说明在超时的范围内，无法等待令牌\n" +
            "\treturn -1\n" +
            "  end\n" +
            "end\n" +
            "--重新计算令牌\n" +
            "if nextTime > nowTime then\n" +
            "  --如果下一次生成令牌的时间大于当前时间，直接返回等待时间\n" +
            "  return nextTime - nowTime\n" +
            "end\n" +
            "if nowTime > nextTime then\n" +
            "  --计算上一次生成令牌到现在的差时\n" +
            "  local hasTime = nowTime -nextTime\n" +
            "  --可以产生的令牌数\n" +
            "  local createTokens = hasTime/singleTokenTime\n" +
            "  --当前总的令牌数\n" +
            "  hasTokens = math.min(hasTokens + createTokens,maxTokens)\n" +
            "  --重新设置下一次可以生成令牌的时间\n" +
            "  nextTime = nowTime\n" +
            "end\n" +
            "--获取令牌\n" +
            "--计算当前能够拿走的令牌\n" +
            "local canGetTokens = math.min(hasTokens,getTokens)\n" +
            "--需要预支的令牌数量\n" +
            "local yuzhiTokens = getTokens - canGetTokens\n" +
            "--计算如果预支这些令牌，需要多少时间（微秒值）\n" +
            "local yuzhiTime = yuzhiTokens * singleTokenTime\n" +
            "--重新设置令牌桶中的值\n" +
            "hasTokens = hasTokens - canGetTokens\n" +
            "--更新令牌桶\n" +
            "redis.call('hmset',key,'hasTokens',hasTokens,'nextTime',nextTime + yuzhiTime)\n" +
            "--返回当前请求需要等待的时间\n" +
            "return nextTime - nowTime";

    public TokenTong(String key, int maxTokens, int secTokens) {
        this.key = key;
        this.maxTokens = maxTokens;
        this.secTokens = secTokens;
        //手动从spring容器中获取redisTemplate
        this.stringRedisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
        //初始化令牌桶
        init();
    }

    /**
     * 初始化令牌桶
     */
    private void init() {
        //初始化到redis中
        //执行lua脚本
        Long execute = stringRedisTemplate.execute(new DefaultRedisScript<Long>(initTokenLua, Long.class),
                Arrays.asList(key),
                maxTokens + "",
                secTokens + "",
                TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) + ""
        );
        System.out.println("执行结果：" + execute);
    }

    //传入领取的令牌数，返回需要等待的时间
    public long getTokens(int tokens) {
        //执行lua脚本
        Long waitTime = stringRedisTemplate.execute(new DefaultRedisScript<Long>(getTokenLua, Long.class),
                Arrays.asList(key),
                tokens + ""
        );
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime / 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return waitTime;
    }

    //传入需要领取的令牌数、超时时间，如果在超时时间内，没办法返回，直接返回失败
    public boolean getTokens(int tokens, int timeout, TimeUnit timeUnit) {
        //执行lua脚本
        Long waitTime = stringRedisTemplate.execute(new DefaultRedisScript<Long>(getTokenLua, Long.class),
                Arrays.asList(key),
                tokens + "",
                timeUnit.toMicros(timeout) + ""
        );
        if (waitTime == -1) {
            return false;
        } else if (waitTime > 0) {
            try {
                Thread.sleep(waitTime / 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    //传入需要领取的令牌数，如果能够立即获取，就返回true，否则返回false
    public boolean getTokensNow(int tokens) {
        return getTokens(tokens, 0, TimeUnit.MICROSECONDS);
    }
}
