package com.zcs.legion.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author lance
 * @since 2019.2.24 17:18
 */
@Data
@RefreshScope
@Component("groupTag")
@ConfigurationProperties(prefix = "tags")
public class GroupTag {
    List<String> modules;
    //List<String> process;
    /**
     * 定义流程列表
     * P://processDefineId
     */
    List<ProcessTag> processDefines;
    List<AgentTag> agentTags;
    Map<String, List<String>> tokenTags;
    Map<String, String> groupIdAndPlatCodes;
    String gatewayPack;
    String legionPackLog;
    List<String> interceptorPath;

    /**
     * 定义代理
     */
    @Data
    public static class AgentTag {
        private String tag;
        private String groupId;
        private String prefix;
    }

    /**
     * 定义流程
     * 2019.8.9
     */
    @Data
    public static class ProcessTag {
        private String tag;
        private String groupId;
    }
}
