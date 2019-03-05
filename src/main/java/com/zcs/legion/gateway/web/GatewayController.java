package com.zcs.legion.gateway.web;

import com.legion.client.common.LegionConnector;
import com.legion.client.common.RequestDescriptor;
import com.legion.client.handlers.SenderHandler;
import com.legion.client.handlers.SenderHandlerFactory;
import com.legion.core.api.X;
import com.legion.core.exception.LegionException;
import com.zcs.legion.gateway.config.GroupTag;
import com.zcs.legion.gateway.result.R;
import com.zcs.legion.gateway.utils.GatewayUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * GatewayController
 * @author lance
 * @since 2019.2.23 15:06
 */
@Slf4j
@RestController
public class GatewayController {
    private final Counter REQUEST_TOTAL = Metrics.counter("http.request.total", "Legion-Gateway", "http.request.total");
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

    /**
     * 消息转发处理(简单消息）
     * @param groupId   GroupID
     * @param tag       标签
     * @param body      消息体
     * @return          返回结果
     */
    @RequestMapping(value = "/{type:m|p}/{groupId}/{tag}", method = RequestMethod.POST)
    public ResponseEntity<R> dispatch(@PathVariable String type, @PathVariable String groupId, @PathVariable String tag,
                            @RequestBody String body, HttpServletRequest request) {
        REQUEST_TOTAL.increment();
        if(log.isDebugEnabled()){
            log.info("===>RequestURI: {}/{}/{}, body: {}", type, groupId, tag, body);
        }

        String contentType = request.getHeader("content-type");
        contentType = StringUtils.isBlank(contentType)? MediaType.APPLICATION_JSON_VALUE: contentType;
        X.XHttpRequest req = GatewayUtils.httpRequest(request, body);

        try {
            RequestDescriptor descriptor = GatewayUtils.create(contentType, tag);
            Single<X.XHttpResponse> response = legionConnector.sendMessage(groupId, descriptor, req, X.XHttpResponse.newBuilder());
            X.XHttpResponse obj = response.blockingGet();
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(R.success(obj.getBody()));
        }catch (Exception ex){
            if(ex instanceof LegionException){
                LegionException exception = (LegionException)ex;
                return ResponseEntity.status(HttpStatus.OK).body(R.error(exception.getCode(), exception.getMessage()));
            }else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.error(ex.getMessage()));
            }
        }
    }

    /**
     * 消息转发处理(代理)固定报文形式
     */
    @RequestMapping(value = "/**/*", method = RequestMethod.POST)
    public ResponseEntity<String> dispatchAgent(HttpServletRequest request, @RequestBody String body){
        GroupTag.AgentTag agentTag = getAgentTag(request.getRequestURI());
        log.info("dispatch agent ,request uri: {}, tag: {}", request.getRequestURI(), agentTag);
        if(agentTag==null){
            return ResponseEntity.badRequest().body("error");
        }

        X.XHttpRequest.Builder agentRequest = X.XHttpRequest.newBuilder();
        agentRequest.setRequestURI(request.getRequestURI());
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            agentRequest.putHeaders(headerName, request.getHeader(headerName));
        }
        agentRequest.setBody(body);

        CompletableFuture<X.XHttpResponse> completableFuture = new CompletableFuture<>();
        SenderHandler<X.XHttpResponse> handler = SenderHandlerFactory.create(success->{
            log.info("response success: {}", success);
            completableFuture.complete(success);
        }, fail->{
            log.info("response failed. {}, {}", fail.getCode(), fail.getMessage());
            String errorMsg = String.format("response failed, code=%d, msg=%s", fail.getCode(), fail.getMessage());
            completableFuture.completeExceptionally(LegionException.valueOf(errorMsg));
        });

        RequestDescriptor descriptor = GatewayUtils.create(agentRequest.getHeadersOrDefault("content-type", MediaType.APPLICATION_JSON_VALUE), agentTag.getTag());
        legionConnector.sendMessage(agentTag.getGroupId(), descriptor, agentRequest.build(), handler, X.XHttpResponse.newBuilder());
        try {
            X.XHttpResponse m = completableFuture.get(1, TimeUnit.MINUTES);
            log.info("completable future completed. m={}", m);
            return ResponseEntity.status(m.getStatus()).contentType(MediaType.APPLICATION_JSON).body(m.getBody());
        } catch (Exception e) {
            log.debug("gateway error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
