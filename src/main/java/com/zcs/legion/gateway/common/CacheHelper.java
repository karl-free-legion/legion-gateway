package com.zcs.legion.gateway.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CacheUtils
 *
 * @author lance
 * 1/8/2020 15:32
 */
@Component
@SuppressWarnings("unchecked")
public class CacheHelper {
    public final static String DEFAULT_KEY = "zcs:commons:gate";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private Set<Object> keysForHash(String key) {
        key = StringUtils.isBlank(key) ? DEFAULT_KEY : key;
        return stringRedisTemplate.opsForHash().keys(key);
    }

    /**
     * 获取第一个key
     */
    public Object keyFirstForHash(String key) {
        Set<Object> keys = keysForHash(key);
        if (Objects.isNull(key) || keys.isEmpty()) {
            return null;
        }

        return keys.iterator().next();
    }

    public String getForHash(String key, Object field) {
        key = StringUtils.isBlank(key) ? DEFAULT_KEY : key;
        return stringRedisTemplate.opsForHash().get(key, field) + "";
    }

    public Map<Object, Object> getMapForHash(String key) {
        return stringRedisTemplate.opsForHash().entries(key);
    }

    /**
     * set[key, value]
     */
    public void setAccessKey(String key, String value) {
        key = StringUtils.isBlank(key) ? DEFAULT_KEY : key;
        accessCache.put(key, value);
    }

    /**
     * 校验是否存在
     */
    public boolean hasAccessKey(String key) {
        return Objects.nonNull(getAccessValue(key));
    }

    /**
     * get Value
     */
    public String getAccessValue(String key) {
        try {
            key = StringUtils.isBlank(key) ? DEFAULT_KEY : key;
            return accessCache.getIfPresent(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 缓存<String, String>4小时
     */
    private Cache<String, String> accessCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(4L))
            .maximumSize(20)
            .build();
}
