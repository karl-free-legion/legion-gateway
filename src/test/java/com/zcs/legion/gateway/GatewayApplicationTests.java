package com.zcs.legion.gateway;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.api.sample.T;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageConversionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
public class GatewayApplicationTests {

    @Test
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

        /*try {
            Object obj = request.getMethod("parseFrom", ByteString.class).invoke(request, ByteString.copyFromUtf8(json));
            log.info("===>{}", obj);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }*/
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
