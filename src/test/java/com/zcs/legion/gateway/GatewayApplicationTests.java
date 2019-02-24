package com.zcs.legion.gateway;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.api.FailResult;
import com.legion.client.api.sample.T;
import com.zcs.legion.gateway.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageConversionException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GatewayApplicationTests {
    private ExecutorService service = Executors.newFixedThreadPool(5);

    @Test
    @Ignore
    public void contextLoads() throws InvalidProtocolBufferException {
        T.SampleMessage message = T.SampleMessage.newBuilder().setMessage("Who are you?").build();
        String json = JsonFormat.printer().print(message);

        Any any = Any.pack(message);
        String json1 = JsonFormat.printer().usingTypeRegistry(JsonFormat.TypeRegistry.newBuilder().add(T.SampleMessage.getDescriptor()).build()).print(any);
        log.info("===>{}, {}", json, json1);

        String key = "accountDelete";
        Map<Class<?extends Message>, Class<?extends Message>> input = Constants.table.row(key);
        Map.Entry<Class<?extends Message>, Class<?extends Message>> entry = input.entrySet().iterator().next();
        Class<?extends Message> request = entry.getKey();

        Message.Builder builder = this.getMessageBuilder(request);
        JsonFormat.parser().merge(json, builder);


        log.info("===>{}", builder.build());
    }


    @Test
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

    private Message.Builder getMessageBuilder(Class<? extends Message> clazz) {
        try {
            Method method = clazz.getMethod("newBuilder");
            return (Message.Builder)method.invoke(clazz);
        } catch (Exception var3) {
            throw new HttpMessageConversionException("Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, var3);
        }
    }
}
