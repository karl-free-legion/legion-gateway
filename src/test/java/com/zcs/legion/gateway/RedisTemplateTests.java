package com.zcs.legion.gateway;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisTemplateTests {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void get(){
        String key = "hello";

        log.info("===>{}", redisTemplate.opsForValue().get(key));
    }
}
