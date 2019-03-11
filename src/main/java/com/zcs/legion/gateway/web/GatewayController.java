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
import org.springframework.http.HttpHeaders;
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
     * 定义请求类型
     * @return ResponseEntity
     */
    @RequestMapping(value = "/{groupId:[A-z]*}/**", method = RequestMethod.POST)
    public ResponseEntity<R> dispatch(@PathVariable String groupId, @RequestBody(required = false)String body, HttpServletRequest request) {
        if(log.isDebugEnabled()){
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }

        ResponseEntity<R>entity;
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId+"/");

        //代理定义, 流程定义, Module请求
        if(groupTag.getAgentTags().stream().anyMatch(a->a.getGroupId().equalsIgnoreCase(groupId))){
            entity = broker(request, body);
        }else if(groupTag.getProcess().contains(groupId)){
            entity = simple("P", groupId, tag, body, request);
        }else {
            entity = simple("M", groupId, tag, body, request);
        }

        return entity;
    }

    /**
     * 消息转发处理(简单消息）
     * @param groupId   GroupID
     * @param tag       标签
     * @param body      消息体
     * @return          返回结果
     */
    @PostMapping(value = "/{type:m|p}/{groupId}/{tag}")
    public ResponseEntity<R> dispatch(@PathVariable String type, @PathVariable String groupId, @PathVariable String tag,
                            @RequestBody(required = false)String body, HttpServletRequest request) {
        REQUEST_TOTAL.increment();
        return simple(type, groupId, tag, body, request);
    }

    /**
     * 定义简单流程
     * @return ResponseEntity
     */
    private ResponseEntity<R> simple(String type, String groupId, String tag, String body, HttpServletRequest request){
        if(log.isDebugEnabled()){
            log.info("===>RequestURI: {}/{}/{}, body: {}", type, groupId, tag, body);
        }

        String contentType = request.getHeader("content-type");
        contentType = StringUtils.isBlank(contentType)? MediaType.APPLICATION_JSON_VALUE: contentType;
        X.XHttpRequest req = GatewayUtils.httpRequest(request, body);

        try {
            RequestDescriptor descriptor = GatewayUtils.create(contentType, tag);
            descriptor.setSource(X.XReqSource.HTTP);
            descriptor.setRequest(req);
            Single<String> response = legionConnector.sendHttpMessage(groupId, descriptor, body);
            String obj = response.blockingGet();
            return ResponseEntity.status(HttpStatus.OK).headers(headers(descriptor)).body(R.success(obj));
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
     * 设置返回headers
     * @param descriptor RequestDescriptor
     * @return          HttpHeaders
     */
    private HttpHeaders headers(RequestDescriptor descriptor){
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(descriptor.getExt());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        return headers;
    }

    /**
     * 代理请求处理
     * @return ResponseEntity
     */
    private ResponseEntity<R> broker(HttpServletRequest request, String body){
        GroupTag.AgentTag agentTag = getAgentTag(request.getRequestURI());
        log.info("dispatch agent ,request uri: {}, tag: {}", request.getRequestURI(), agentTag);
        if(agentTag == null){
            return ResponseEntity.badRequest().body(R.error(-100, "Error."));
        }

        X.XHttpRequest.Builder agentRequest = X.XHttpRequest.newBuilder();
        agentRequest.setRequestURI(request.getRequestURI());
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            agentRequest.putHeaders(headerName, request.getHeader(headerName));
        }
        //agentRequest.setBody(body);

        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        SenderHandler<String> handler = SenderHandlerFactory.create(
                completableFuture::complete, fail->{
            log.info("response failed. {}, {}", fail.getCode(), fail.getMessage());
            String errorMsg = String.format("response failed, code=%d, msg=%s", fail.getCode(), fail.getMessage());
            completableFuture.completeExceptionally(LegionException.valueOf(errorMsg));
        });

        RequestDescriptor descriptor = GatewayUtils.create(agentRequest.getHeadersOrDefault("content-type", MediaType.APPLICATION_JSON_VALUE), agentTag.getTag());
        //legionConnector.sendMessage(agentTag.getGroupId(), descriptor, agentRequest.build(), handler, X.XHttpResponse.newBuilder());
        try {
            String m = completableFuture.get(1, TimeUnit.MINUTES);
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON_UTF8).body(R.success(m));
        } catch (Exception e) {
            log.warn("gateway error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.error(-100, e.getMessage()));
        }
    }
}
