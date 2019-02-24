package com.zcs.legion.gateway.web;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.api.FailResult;
import com.legion.client.common.LegionConnector;
import com.legion.client.handlers.SenderHandler;
import com.legion.client.handlers.SenderHandlerFactory;
import com.legion.core.XHelper;
import com.zcs.legion.gateway.config.Constants;
import com.zcs.legion.gateway.result.R;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GatewayController
 * @author lance
 * @since 2019.2.23 15:06
 */
@Slf4j
@RestController
public class GatewayController {
    private final Counter REQUEST_TOTAL = Metrics.counter(" http_req_total", "Legion-Gateway", "reg_node_total");
    @Autowired
    private LegionConnector connector;

    /**
     * 消息转发处理
     * @param type      消息类型, M/P
     * @param groupId   GroupID
     * @param tag       标签
     * @param body      消息体
     * @return          返回结果
     */
    @RequestMapping(value = "/{type}/{groupId}/{tag}", method = RequestMethod.POST)
    public R dispatch(@PathVariable String type, @PathVariable String groupId,
                      @PathVariable String tag, @RequestBody String body) {
        REQUEST_TOTAL.increment();

        if(log.isDebugEnabled()){
            log.info("===>RequestURI: {}/{}/{}, body: {}", type, groupId, tag, body);
        }

        //根据key, 获取注册到legion上Module
        String key = groupId + StringUtils.capitalize(tag);
        Map<Class<?extends Message>, Class<?extends Message>> input = Constants.table.row(key);
        if(input == null || input.isEmpty()){
            log.warn("===>Current key not register legion{}", key);
            return R.error(-1000, "请求的Module尚未注册.");
        }

        //根据key获取RequestMessage/ReplyMessage
        Map.Entry<Class<?extends Message>, Class<?extends Message>> entry = input.entrySet().iterator().next();
        Class<?extends Message> request = entry.getKey();
        Class<?extends Message> reply = entry.getValue();

        final CompletableFuture<Message> successful = new CompletableFuture<>();
        final CompletableFuture<FailResult> failure = new CompletableFuture<>();

        //发送消息
        Message.Builder builder = XHelper.messageBuilder(body, request);
        SenderHandler handler = SenderHandlerFactory.create(successful::complete, failure::complete);

        //发送完成, 异步等待结果
        connector.sendMessage(groupId, tag, builder.build(), handler, reply);
        Object result = CompletableFuture.anyOf(successful, failure).join();
        if(result instanceof Message){
            try {
                result = JsonFormat.printer().print((Message)result);
            } catch (InvalidProtocolBufferException e) {
                log.error("===>protoBuf Message -> Json error. ", e);
            }
        }
        return R.success(result);
    }
}
