package com.zcs.legion.gateway.utils;

import com.legion.client.common.RequestDescriptor;
import com.legion.core.api.X;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * GatewayUtils
 * @author lance
 * @since 2019.3.1 16:16
 */
public final class GatewayUtils {

    /**
     * ApplicationType类型转换XDataFormat
     * @param contentType  ApplicationType
     * @return  X.XDataFormat
     */
    private static X.XDataFormat toFormat(String contentType){
        switch (contentType){
            case MediaType.APPLICATION_JSON_UTF8_VALUE:
            case MediaType.APPLICATION_JSON_VALUE: return X.XDataFormat.JSON;
            case MediaType.APPLICATION_XML_VALUE:  return X.XDataFormat.XML;
            case "application/protobuf":
            case "application/pb": return X.XDataFormat.PB;
            default: return X.XDataFormat.JSON;
        }
    }

    /**
     * X.XDataFormat -> MediaType
     * @param format XDataFormat
     * @return  String
     */
    private static String fromFormat(X.XDataFormat format){
        switch (format.getNumber()){
            case X.XDataFormat.PB_VALUE: return "application/pb";
            case X.XDataFormat.XML_VALUE: return MediaType.APPLICATION_XML_VALUE;
            case X.XDataFormat.JSON_VALUE:
            default: return MediaType.APPLICATION_JSON_UTF8_VALUE;
        }
    }

    /**
     * 创建XHttpRequest
     * @param request HttpServletRequest
     * @param body    请求body
     * @return        XHttpRequest
     */
    public static X.XHttpRequest httpRequest(HttpServletRequest request, String body){
        X.XHttpRequest.Builder builder = X.XHttpRequest.newBuilder()
                .setBody(body)
                .setRequestURI(request.getRequestURI());

        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            builder.putHeaders(headerName, request.getHeader(headerName));
        }
        return builder.build();
    }

    /**
     * 创建RequestDescriptor
     * @param contentType contentType
     * @param tag         tag
     * @return            RequestDescriptor
     */
    public static RequestDescriptor create(String contentType, String tag){
        //X.XDataFormat format = toFormat(contentType);
        X.XDataFormat format = X.XDataFormat.PB;
        return RequestDescriptor.builder()
                .acceptType(format)
                .contentType(format)
                .lang(X.XLang.CN)
                .timezone(X.XTimezone.UTC8)
                .tag(tag)
                .build();
    }
}
