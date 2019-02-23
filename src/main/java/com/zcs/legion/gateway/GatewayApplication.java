package com.zcs.legion.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

/**
 * Legion-Gateway
 * @author lance
 * @since 2019.2.23 16:41
 */
@SpringBootApplication(scanBasePackages = {"com.zcs.legion.gateway", "com.legion.client"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
