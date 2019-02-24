package com.zcs.legion.gateway;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.api.FailResult;
import com.legion.client.api.sample.T;
import com.zcs.legion.gateway.result.R;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageConversionException;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GatewayApplicationTests {
    private ExecutorService service = Executors.newFixedThreadPool(5);

    @Test
    @Ignore
    public void future(){
        final FailResult result = FailResult.builder().code(2000).message("Hello World.").build();
        final CompletableFuture<String> successful = new CompletableFuture<>();
        final CompletableFuture<FailResult> failure = new CompletableFuture<>();

        service.submit(()->{
            successful.complete("2222");
            failure.complete(FailResult.builder().code(1000).message("Hello").build());
        });

        Object obj = CompletableFuture.anyOf(failure, successful).join();
        log.info("===>{}", obj);
    }

    @Test
    @Ignore
    public void toJsonObject() throws InvalidProtocolBufferException {
        String item = "{\n" +
                "    \"code\": 0,\n" +
                "    \"message\": \"{\\n  \\\"message\\\": \\\"Amazed! Query Time: 2019-02-24 14:42:54\\\"\\n}\"\n" +
                "}";

        FailResult fail = FailResult.builder().code(1000).message("Hello").build();
        R result = R.success(fail);

        T.SampleMessage message = T.SampleMessage.newBuilder().setMessage("OK").build();
        String messageJson = JsonFormat.printer().print(message.toBuilder());

        log.info("===>R: {}", JSON.toJSONString(result));
        log.info("===>M: {}", JSON.parseObject(messageJson, M.class));
        log.info("===>{}", JSON.parseObject(item, R.class));
    }

    @Test
    public void completeFuture() throws ExecutionException, InterruptedException {
        CompletableFuture<Object> future = new CompletableFuture<>();
        FailResult fail = FailResult.builder().code(1000).message("Hello").build();
        service.submit(()->{
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //future.complete("hello world.");
            future.completeExceptionally(new RuntimeException("xxx"));
        });

        log.info("===>{}", future.get());
    }

    private Message.Builder getMessageBuilder(Class<? extends Message> clazz) {
        try {
            Method method = clazz.getMethod("newBuilder");
            return (Message.Builder)method.invoke(clazz);
        } catch (Exception var3) {
            throw new HttpMessageConversionException("Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, var3);
        }
    }

    @Data
    static class M{
        String message;
    }
}
