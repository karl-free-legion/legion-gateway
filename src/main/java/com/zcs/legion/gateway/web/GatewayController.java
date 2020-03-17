package com.zcs.legion.gateway.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.util.JsonFormat;
import com.legion.client.common.LegionConnector;
import com.legion.client.common.RequestDescriptor;
import com.legion.client.utils.MessageUtils;
import com.legion.core.api.B;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
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

    @ResponseBody
    @RequestMapping(value = "/{groupId:[A-z|0-9]*}/**", consumes = {MediaType.TEXT_PLAIN_VALUE,
            MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> textPlain(@PathVariable String groupId, @RequestBody(required = false) String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }

        body = StringUtils.isBlank(body) ? " " : body;
        body = MessageUtils.toJson(B.RawMessage.newBuilder().setData(body));

        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");
        ResponseEntity<R> entity = simple("M", groupId, tag, body, request);
        String message = entity.getBody().get("message") + "";
        if (StringUtils.isNotBlank(message)) {
            B.RawMessage.Builder rawMessage = (B.RawMessage.Builder) MessageUtils.json2Message(message, B.RawMessage.class);
            return ResponseEntity.status(HttpStatus.OK).body(rawMessage.getData());
        }
        return ResponseEntity.status(HttpStatus.OK).body("handler fail");
    }

    /**
     * 定义请求类型
     *
     * @return ResponseEntity
     */
    @ResponseBody
    @RequestMapping(value = "/{groupId:[A-z|0-9]*}/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<R> dispatch(@PathVariable String groupId, @RequestBody(required = false) String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }
        ResponseEntity<R> entity;
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");
        //代理定义, 流程定义ProcessTag, Module请求
        if (groupTag.getAgentTags().stream().anyMatch(a -> a.getTag().equalsIgnoreCase(groupId))) {
            entity = broker(request, body);
        } else if (groupTag.getProcessDefines().stream().anyMatch(a -> a.getGroupId().equalsIgnoreCase(groupId) && a.getTag().equalsIgnoreCase(tag))) {
            entity = simple("P", groupId, tag, body, request);
        } else {
            entity = simple("M", groupId, tag, body, request);
        }

        return entity;
    }

    /**
     * 消息转发处理(简单消息）
     *
     * @param groupId GroupID
     * @param tag     标签
     * @param body    消息体
     * @return 返回结果
     */
    @ResponseBody
    @PostMapping(value = "/{type:m|p}/{groupId}/{tag}")
    public ResponseEntity<R> dispatch(@PathVariable String type, @PathVariable String groupId, @PathVariable String tag,
                                      @RequestBody(required = false) String body, HttpServletRequest request) {
        REQUEST_TOTAL.increment();
        return simple(type, groupId, tag, body, request);
    }

    /**
     * 可通过该接口查看配置是否更新
     */
    @ResponseBody
    @RequestMapping(value = "/checkProp", method = {RequestMethod.GET, RequestMethod.POST})
    public void checkProperties() {
        log.info(JSON.toJSONString(groupTag));
    }


    /**
     * 浏览器重定向
     *
     * @param groupId
     * @param body
     * @param request
     * @return
     */
    @RequestMapping(value = "/redirect/{groupId:[A-z|0-9]*}/**", method = {RequestMethod.GET, RequestMethod.POST})
    public String redirect(@PathVariable String groupId, @RequestBody(required = false) String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>GroupId: {}, tag: {}", groupId, request.getRequestURI());
        }
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");
        Map<String, String> resultMap = redirectSimple("M", groupId, tag, body, request);
        if (resultMap.get(REDIRECT_URL) != null) {
            return "redirect:" + resultMap.get(REDIRECT_URL);
        }
        return "error";
    }

    /**
     * 浏览器重定向接收参数(pay服务渠道支付专用)
     * @param groupId
     * @param body
     * @param request
     * @return
     */
    @RequestMapping(value = "/redirect/data/{groupId:[A-z|0-9]*}/**", method = {RequestMethod.GET, RequestMethod.POST})
    public void redirectForm(@PathVariable String groupId, @RequestBody(required = false) String body, HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        log.info("===>GroupId: {}, redirect tag: {}", groupId, request.getRequestURI());

        if(!CollectionUtils.isEmpty(request.getParameterMap()) && StringUtils.isBlank(body)){
            StringBuilder builder = new StringBuilder();
            request.getParameterMap().forEach((k , v)->{
                builder.append(k+"=");
                builder.append(v[0]+"&");
            });
            body = builder.toString();
        }
        body = MessageUtils.toJson(B.RawMessage.newBuilder().setData(StringUtils.isBlank(body)?"":body));
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");
        X.XHttpRequest req = GatewayUtils.httpRequest(request);
        RequestDescriptor descriptor = GatewayUtils.create(request.getHeader("content-type"), tag);
        descriptor.setSource(X.XReqSource.HTTP);
        descriptor.setRequest(req);
        String s = legionConnector.sendHttpMessage(groupId, descriptor, body).blockingGet().toString();
        Map<String, String> resultMap = JSON.parseObject(s, Map.class);
        if (StringUtils.isNotBlank(resultMap.get(REDIRECT_URL))) {
            response.sendRedirect(resultMap.get(REDIRECT_URL));
        }
    }

    /**
     * 定义简单流程
     *
     * @return ResponseEntity
     */
    private ResponseEntity<R> simple(String type, String groupId, String tag, String body, HttpServletRequest request) {
        if (log.isInfoEnabled()) {
            log.info("===>RequestURI: {}/{}/{}", type, groupId, tag);
        }

        String contentType = request.getHeader("content-type");
        contentType = StringUtils.isBlank(contentType) ? MediaType.APPLICATION_JSON_VALUE : contentType;
        X.XHttpRequest req = GatewayUtils.httpRequest(request);

        try {
            RequestDescriptor descriptor = GatewayUtils.create(contentType, tag);
            descriptor.setSource(X.XReqSource.HTTP);
            descriptor.setRequest(req);

            //如果是流程, defineId = tag, 此时group/tag意义不大, 关键是根据defineId找流程
            //获取流程中定义的group/tag
            if (type.equalsIgnoreCase("P")) {
                descriptor.setProcessDefine(true);
                groupId = tag;
            }

            Single<String> response = legionConnector.sendHttpMessage(groupId, descriptor, body);
            String obj = response.blockingGet();
            log.info("===>ReplyURI: {}/{}/{}", type, groupId, tag);
            return ResponseEntity.status(HttpStatus.OK).headers(headers(descriptor)).body(R.success(obj));
        } catch (Exception ex) {
            log.warn("===>{}, fail: ", ex.getMessage(), ex);
            if (ex instanceof LegionException) {
                LegionException exception = (LegionException) ex;
                R result;
                if (StringUtils.isNotBlank(exception.getErrorCode())) {
                    result = R.error(exception.getCode(), exception.getErrorCode(), exception.getMessage());
                } else {
                    result = R.error(exception.getCode(), exception.getMessage());
                }
                return ResponseEntity.status(HttpStatus.OK).body(result);
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(R.error(ExceptionConstants.TIME_OUT.getCode(), ExceptionConstants.TIME_OUT.getValue()));
            }
        }
    }

    /**
     * 请求结束后进行重定向
     *
     * @param type
     * @param groupId
     * @param tag
     * @param body
     * @param request
     * @return
     */
    private Map<String, String> redirectSimple(String type, String groupId, String tag, String body, HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.info("===>RequestURI: {}/{}/{}", type, groupId, tag);
        }
        try {
            String s = sendToModel(request, tag, groupId, body);
            return JSON.parseObject(s, Map.class);
        } catch (Exception ex) {
            log.warn("===>{}", ex.getMessage(), ex);
            return new HashMap<>();
        }
    }

    /**
     * 将请求发到model
     *
     * @param request
     * @param tag
     * @param groupId
     * @param body
     * @return
     */
    private String sendToModel(HttpServletRequest request, String tag, String groupId, String body) {
        String contentType = request.getHeader("content-type");
        contentType = StringUtils.isBlank(contentType) ? MediaType.APPLICATION_JSON_VALUE : contentType;
        body = contentType.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE) ? body : "";
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
        Map<String, String> source = new HashMap<>(descriptor.getRequest().getHeadersMap());
        if (source != null && source.containsKey("content-length")) {
            source.remove("content-length");
        }
        headers.setAll(source);
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
