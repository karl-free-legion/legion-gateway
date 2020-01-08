package com.zcs.legion.gateway.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * CacheUtils
 *
 * @author lance
 * 1/8/2020 15:32
 */
@Component
public class CacheUtils {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 判断key是否存在
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
