package com.zcs.legion.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

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
    List<String> modules;
    List<String> process;
    List<AgentTag> agentTags;

    @Data
    public static class AgentTag{
        private String tag;
        private String groupId;
        private String prefix;
    }
}
