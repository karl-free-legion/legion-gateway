package com.zcs.legion.gateway.commons;

import com.zcs.legion.gateway.common.CacheHelper;
import com.zcs.legion.gateway.utils.DateUtils;
import com.zcs.legion.gateway.utils.DecryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author lance
 * 1/9/2020 17:46
 */
@Slf4j
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CacheHelperTests {
    @Autowired
    private CacheHelper cacheHelper;

    @Test
    public void run() throws InterruptedException {
        cacheHelper.setAccessKey(CacheHelper.DEFAULT_KEY, "222");
        Thread.sleep(1000L);
        boolean result = cacheHelper.hasAccessKey(CacheHelper.DEFAULT_KEY);
        String value = cacheHelper.getAccessValue(CacheHelper.DEFAULT_KEY);

        log.info("===> result: {}, Value: {}", result, value);

        Object field = cacheHelper.keyFirstForHash(CacheHelper.DEFAULT_KEY);
        Object nullKey = cacheHelper.keyFirstForHash("Jim");
        String keyValue = cacheHelper.getForHash(CacheHelper.DEFAULT_KEY, field);
        log.info("===>field: {}, value: {}, nullKey: {}", field, keyValue, nullKey);

        Map<Object, Object> entries = cacheHelper.getMapForHash(CacheHelper.DEFAULT_KEY);

        String expireAt = null;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String key = entry.getKey() + "";
            String val = entry.getValue() + "";
            expireAt = DecryptUtils.decrypt(val, CacheHelper.DEFAULT_KEY, key.getBytes(StandardCharsets.UTF_8));
        }

        String expire = DecryptUtils.decrypt(keyValue, CacheHelper.DEFAULT_KEY, field.toString().getBytes());
        log.info("===>Expire: {}, expireAt:{}, CompareNow: {}", expire, expireAt, DateUtils.compare(expire));
    }
}
