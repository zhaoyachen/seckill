package com.zyc.gateway.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class LockUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 加锁的lua脚本
     */
    private String lockLua = "--锁的名称\n" +
            "local lockName=KEYS[1]\n" +
            "--锁的value\n" +
            "local lockValue=ARGV[1]\n" +
            "--过期时间 秒\n" +
            "local timeout=tonumber(ARGV[2])\n" +
            "--尝试进行加锁\n" +
            "local flag=redis.call('setnx',lockName,lockValue)\n" +
            "--判断是否获得锁\n" +
            "if flag==1 then\n" +
            "  --获得分布式锁，设置过期时间\n" +
            "  redis.call('expire',lockName,timeout)\n" +
            "end\n" +
            "--返回标识\n" +
            "return flag";

    /**
     * 解锁的Lua脚本
     */
    private String unLockLua = "--锁的名称\n" +
            "local lockName=KEYS[1]\n" +
            "--锁的value\n" +
            "local lockValue=ARGV[1]\n" +
            "--判断锁是否存在，以及锁的内容是否为自己加的\n" +
            "local value=redis.call('get',lockName)\n" +
            "--判断是否相同\n" +
            "if value == lockValue then\n" +
            "  redis.call('del',lockName)\n" +
            "  return 1\n" +
            "end\n" +
            "return 0";

    private ThreadLocal<String> tokens = new ThreadLocal<>();

    public void lock(String lockName) {
        lock(lockName, 30);
    }

    public void lock(String lockName, Integer timeout) {
        String token = UUID.randomUUID().toString();
        //设置给threadLocal
        tokens.set(token);
        Long flag = redisTemplate.execute(new DefaultRedisScript<Long>(lockLua, Long.class),
                Arrays.asList(lockName),
                token,
                timeout + ""
        );
        //设置锁的自旋
        if (flag == 0) {
            //未获得锁
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lock(lockName, timeout);
        }
    }

    public boolean unlock(String lockName) {
        String token = tokens.get();
        Long flag = redisTemplate.execute(new DefaultRedisScript<Long>(unLockLua, Long.class),
                Arrays.asList(lockName),
                token
        );
        return flag == 1;
    }


}
