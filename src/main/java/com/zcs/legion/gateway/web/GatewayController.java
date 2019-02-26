package com.zcs.legion.gateway.web;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.api.FailResult;
import com.legion.client.common.LegionConnector;
import com.legion.client.handlers.SenderHandler;
import com.legion.client.handlers.SenderHandlerFactory;
import com.legion.core.LegionException;
import com.legion.core.XHelper;
import com.legion.core.api.X;
import com.zcs.legion.gateway.config.GroupTag;
import com.zcs.legion.gateway.result.R;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private LegionConnector legionConnector;
    @Autowired
    private GroupTag groupTag;


    private GroupTag.AgentTag getAgentTag(String requestURI){
        for(int i=0;i<groupTag.getAgentTags().size();i++){
            GroupTag.AgentTag at = groupTag.getAgentTags().get(i);
            if(requestURI.startsWith(at.getPrefix())){
                return at;
            }
        }
        return null;
    }

    private R dispatchInternal(String type, String groupId, String tag, String body){
        REQUEST_TOTAL.increment();

        if(log.isDebugEnabled()){
            log.info("===>RequestURI: {}/{}/{}, body: {}", type, groupId, tag, body);
        }

        //根据key, 获取注册到legion上Module
        List<GroupTag.GroupTable> tables = groupTag.getGroups().get(groupId);
        Optional<GroupTag.GroupTable>table = tables.stream().filter(e->e.getTag().equalsIgnoreCase(tag)).findFirst();
        if(!table.isPresent()){
            return R.error(-1000, "请求的Module尚未注册.");
        }

        Class<?extends Message> request = table.get().getInput();
        Class<?extends Message> reply = table.get().getOutput();

        final CompletableFuture<Message> successful = new CompletableFuture<>();
        final CompletableFuture<FailResult> failure = new CompletableFuture<>();

        //发送消息
        Message.Builder builder = XHelper.messageBuilder(body, request);
        SenderHandler handler = SenderHandlerFactory.create(successful::complete, failure::complete);

        //发送完成, 异步等待结果
        legionConnector.sendMessage(groupId, tag, builder.build(), handler, reply);
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

    /**
     * 消息转发处理(简单消息）
     * @param groupId   GroupID
     * @param tag       标签
     * @param body      消息体
     * @return          返回结果
     */
    @RequestMapping(value = "/m/{groupId}/{tag}", method = RequestMethod.POST)
    public R dispatchSimple(@PathVariable String groupId,
                      @PathVariable String tag, @RequestBody String body) {
        return dispatchInternal("M", groupId, tag, body);
    }
    /**
     * 消息转发处理（带流程的消息）
     * @param groupId   GroupID
     * @param tag       标签
     * @param body      消息体
     * @return          返回结果
     */
    @RequestMapping(value = "/p/{groupId}/{tag}", method = RequestMethod.POST)
    public R dispatchProcess(@PathVariable String groupId,
                            @PathVariable String tag, @RequestBody String body) {
        return dispatchInternal("P", groupId, tag, body);
    }

    /**
     * 消息转发处理（代理）
     * @param request
     * @param body
     * @return
     */
    @RequestMapping(value = "/**/*", method = RequestMethod.POST)
    public ResponseEntity<String> dispatchAgent(HttpServletRequest request, @RequestBody String body){

        GroupTag.AgentTag agentTag = getAgentTag(request.getRequestURI());
        log.info("dispatch agent ,request uri: {}, tag: {}", request.getRequestURI(), agentTag);
        if(agentTag==null){
            return ResponseEntity.badRequest().body("error");
        }
//        request.get
        X.XAgentRequest.Builder agentRequest = X.XAgentRequest.newBuilder();
        agentRequest.setRequest(request.getRequestURI());
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            agentRequest.putHeaders(headerName, request.getHeader(headerName));
        }
        agentRequest.setBody(ByteString.copyFromUtf8(body));

        CompletableFuture<X.XAgentResponse> completableFuture = new CompletableFuture<>();
        SenderHandler<X.XAgentResponse> handler = SenderHandlerFactory.create(success->{
            //handler success
            log.info("response success: {}", success);
            completableFuture.complete(success);
        }, fail->{
            //handler
            log.info("response failed. {}, {}", fail.getCode(), fail.getMessage());
            String errorMsg = String.format("response failed, code=%d, msg=%s", fail.getCode(), fail.getMessage());
            completableFuture.completeExceptionally(LegionException.valueOf(errorMsg));
        });

        log.info("===> start message.");
        legionConnector.sendMessage(agentTag.getGroupId(), agentTag.getTag(), agentRequest.build(), handler, X.XAgentResponse.class);
        try {
            X.XAgentResponse m = completableFuture.get(1, TimeUnit.MINUTES);
            log.info("completable future completed. m={}", m);
            return ResponseEntity.status(m.getStatus()).contentType(MediaType.APPLICATION_JSON).body(m.getBody().toStringUtf8());
        } catch (Exception e) {
            log.debug("gateway error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());

        }
    }
}
