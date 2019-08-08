package com.zcs.legion.gateway;

import com.zcsmart.ccks.SE;
import com.zcsmart.ccks.SEFactory;
import com.zcsmart.ccks.exceptions.SecurityLibExecption;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;

@Slf4j
public class CcksTests {

    @Test
    public void app() throws IOException, SecurityLibExecption {
        SE se = SEFactory.init("/home/pack/lgm-gateway-server-test.pack", null, "/home/logs/ccks.log");

        log.info("===>ccksId: {}, domain: {}", se.getCurrentId(), se.getCurrentDomainName());
    }
}
