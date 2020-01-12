package com.zcs.legion.gateway.commons;

import com.zcs.legion.gateway.utils.DateUtils;
import com.zcs.legion.gateway.utils.DecryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * 用来生成加密和Redis键值对
 *
 * @author lance
 * 1/9/2020 19:00
 */
@Slf4j
public class AesTests {

    @Test
    public void run() {
        //Access Expire
        String originalString = "2020-01-20";
        String secretKey = "zcs:commons:gate";
        String field = RandomStringUtils.randomAlphanumeric(16);

        byte[] iv = field.getBytes(StandardCharsets.UTF_8);

        String encryptedString = AES.encrypt(originalString + " 23:59:59", secretKey, iv);
        String decryptedString = DecryptUtils.decrypt(encryptedString, secretKey, iv);

        log.info("===>encrypted: {}, decrypted: {}, result: {}", encryptedString, decryptedString, DateUtils.compare(decryptedString));

        log.info("===> DEL {}", secretKey);
        log.info("===> HSET {} {} {}", secretKey, field, encryptedString);
        log.info("===> EXPIREAT {} {}", secretKey, DateUtils.timestamp(decryptedString));
    }
}
