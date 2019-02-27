package com.zcs.legion.gateway.config;

import com.google.protobuf.Message;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author lance
 * @since 2019.2.24 17:18
 */
@Data
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tags")
public class GroupTag {
    Map<String, List<GroupTable>> groups;
    List<AgentTag> agentTags;

    @Data
    public static class GroupTable{
        private String tag;
        private Class<? extends Message> input;
        private Class<? extends Message> output;
    }

    @Data
    public static class AgentTag{
        private String tag;
        private String groupId;
        private String prefix;
    }
}
