package com.zcs.legion.gateway.web;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.common.LegionConnector;
import com.legion.client.common.RequestDescriptor;
import com.legion.core.api.X;
import com.legion.core.exception.LegionException;
import com.legion.net.exception.ExceptionConstants;
import com.zcs.legion.api.A;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * GatewayController
 *
 * @author lance
 * @since 2019.2.23 15:06
 */
@Slf4j
@Controller
public class GatewayController {
    private final Counter REQUEST_TOTAL = Metrics.counter("http.request.total", "Legion-Gateway", "http.request.total");
    @Autowired
    private LegionConnector legionConnector;
    @Autowired
    private GroupTag groupTag;
    /**
     * 重定向的url
     */
    private final String REDIRECT_URL = "url";

    private GroupTag.AgentTag getAgentTag(String requestURI) {
        for (int i = 0; i < groupTag.getAgentTags().size(); i++) {
            GroupTag.AgentTag at = groupTag.getAgentTags().get(i);
            if (requestURI.startsWith(at.getPrefix())) {
                return at;
            }
        }
        return null;
    }

    /**
     * 定义请求类型
     *
     * @return ResponseEntity
     */
    @RequestMapping(value = "/{groupId:[A-z|0-9]*}/**")
    @ResponseBody
    public ResponseEntity<R> dispatch(@PathVariable String groupId, @RequestBody(required = false) String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }

        ResponseEntity<R> entity;
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");

        //代理定义, 流程定义, Module请求
        if (groupTag.getAgentTags().stream().anyMatch(a -> a.getTag().equalsIgnoreCase(groupId))) {
            entity = broker(request, body);
        } else if (groupTag.getProcess().contains(groupId)) {
            entity = simple("P", groupId, tag, body, request);
        } else {
            entity = simple("M", groupId, tag, body, request);
        }

        return entity;
    }

/*    *//**
     * 定义微信扫码请求
     * @return ResponseEntity
     *//*
    @RequestMapping(value = "/{groupId:[a-z]}/{qrCode}")
    public ResponseEntity<R> wxSweepDispatch(@PathVariable String groupId, @PathVariable String qrCode, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }
        Map<String,String> paramMap = new HashMap<>(2);
        paramMap.put("qrCode",qrCode);
        paramMap.put("type", groupId);
        return simple("M", "exhibition", "code", JSON.toJSONString(paramMap), request);
    }*/

    /**
     * 消息转发处理(简单消息）
     *
     * @param groupId GroupID
     * @param tag     标签
     * @param body    消息体
     * @return 返回结果
     */
    @PostMapping(value = "/{type:m|p}/{groupId}/{tag}")
    @ResponseBody
    public ResponseEntity<R> dispatch(@PathVariable String type, @PathVariable String groupId, @PathVariable String tag,
                                      @RequestBody(required = false) String body, HttpServletRequest request) {
        REQUEST_TOTAL.increment();
        return simple(type, groupId, tag, body, request);
    }

    /**
     * 浏览器重定向
     * @param groupId
     * @param body
     * @param request
     * @return
     */
    @RequestMapping(value = "/redirect/{groupId:[A-z|0-9]*}/**")
    public String redirect(@PathVariable String groupId,@RequestBody(required = false) String body, HttpServletRequest request){
        if (log.isDebugEnabled()) {
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");
        Map<String,String> resultMap= redirectSimple("M", groupId, tag, body, request);
        if(resultMap.get(REDIRECT_URL)!=null){
            return "redirect:"+resultMap.get(REDIRECT_URL);
        }
        return "error";
    }
    /**
     * 定义简单流程
     *
     * @return ResponseEntity
     */
    private ResponseEntity<R> simple(String type, String groupId, String tag, String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>RequestURI: {}/{}/{}, body: {}", type, groupId, tag, body);
        }

        String contentType = request.getHeader("content-type");
        contentType = StringUtils.isBlank(contentType) ? MediaType.APPLICATION_JSON_VALUE : contentType;
        X.XHttpRequest req = GatewayUtils.httpRequest(request);

        try {
            RequestDescriptor descriptor = GatewayUtils.create(contentType, tag);
            descriptor.setSource(X.XReqSource.HTTP);
            descriptor.setRequest(req);
            Single<String> response = legionConnector.sendHttpMessage(groupId, descriptor, body);
            String obj = response.blockingGet();
            return ResponseEntity.status(HttpStatus.OK).headers(headers(descriptor)).body(R.success(obj));
        } catch (Exception ex) {
            log.warn("===>{}", ex.getMessage(), ex);
            if (ex instanceof LegionException) {
                LegionException exception = (LegionException) ex;
                return ResponseEntity.status(HttpStatus.OK).body(R.error(exception.getCode(), exception.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(R.error(ExceptionConstants.TIME_OUT.getCode(), ExceptionConstants.TIME_OUT.getValue()));
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.error(ex.getMessage()));
            }
        }
    }

    /**
     * 请求结束后进行重定向
     * @param type
     * @param groupId
     * @param tag
     * @param body
     * @param request
     * @return
     */
    private Map<String,String> redirectSimple(String type, String groupId, String tag, String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>RequestURI: {}/{}/{}, body: {}", type, groupId, tag, body);
        }
        try {
            String s = sendToModel(request,tag,groupId,body);
            return JSON.parseObject(s,Map.class);
        } catch (Exception ex) {
            log.warn("===>{}", ex.getMessage(), ex);
            return new HashMap<>();
        }
    }

    /**
     * 将请求发到model
     * @param request
     * @param tag
     * @param groupId
     * @param body
     * @return
     */
    private String sendToModel( HttpServletRequest request,String tag,String groupId,String body){
        String contentType = request.getHeader("content-type");
        contentType = StringUtils.isBlank(contentType) ? MediaType.APPLICATION_JSON_VALUE : contentType;
        body = contentType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)?body:"";
        X.XHttpRequest req = GatewayUtils.httpRequest(request);
        RequestDescriptor descriptor = GatewayUtils.create(contentType, tag);
        descriptor.setSource(X.XReqSource.HTTP);
        descriptor.setRequest(req);
        Single<String> response = legionConnector.sendHttpMessage(groupId, descriptor, body);
        return response.blockingGet();
    }
    /**
     * 设置返回headers
     *
     * @param descriptor RequestDescriptor
     * @return HttpHeaders
     */
    private HttpHeaders headers(RequestDescriptor descriptor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAll(descriptor.getExt());
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
        return headers;
    }

    /**
     * 代理请求处理
     *
     * @return ResponseEntity
     */
    private ResponseEntity<R> broker(HttpServletRequest request, String body) {
        GroupTag.AgentTag agentTag = getAgentTag(request.getRequestURI());
        log.info("dispatch agent ,request uri: {}, tag: {}", request.getRequestURI(), agentTag);
        if (agentTag == null) {
            return ResponseEntity.badRequest().body(R.error(-100, "Error."));
        }

        try {
            X.XHttpRequest agentRequest = GatewayUtils.httpRequest(request);
            A.BrokerMessage message = A.BrokerMessage.newBuilder().setBody(body).build();
            RequestDescriptor descriptor = GatewayUtils.create(MediaType.APPLICATION_JSON_VALUE, agentTag.getTag());
            descriptor.setSource(X.XReqSource.HTTP);
            descriptor.setRequest(agentRequest);

            Single<String> response = legionConnector.sendHttpMessage("agent", descriptor, JsonFormat.printer().print(message));
            String obj = response.blockingGet();

            A.BrokerMessage.Builder result = A.BrokerMessage.newBuilder();
            JsonFormat.parser().merge(obj, result);
            return ResponseEntity.status(HttpStatus.OK).headers(headers(descriptor)).body(R.success(result.getCode(), result.getBody()));
        } catch (Exception e) {
            log.warn("===>{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.OK).body(R.error(ExceptionConstants.TIME_OUT.getCode(), ExceptionConstants.TIME_OUT.getValue()));
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.error(-100, e.getMessage()));
        }
    }
}
