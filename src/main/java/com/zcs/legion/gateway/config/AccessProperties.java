package com.zcs.legion.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * 访问控制
 *
 * @author lance
 * 1/8/2020 10:24
 */
@Configuration
public class AccessProperties {
    /**
     * 是否开启
     */
    @Value("${zcs.commons.access.enable:true}")
    private boolean enable;
    /**
     * 失效时间(单位：s)
     */
    @Value("${zcs.commons.access.expire:3600}")
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration expire = Duration.ofMillis(60 * 60);

    public boolean isEnable() {
        return enable;
    }

    public Duration getExpire() {
        return expire;
    }
}
