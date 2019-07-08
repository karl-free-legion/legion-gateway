package com.zcs.legion.gateway.config;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：lyj
 * @date ：2019/7/2 10:45
 * @description：
 */
@Slf4j
@Configuration
@ComponentScan("com.zcs.legion.gateway.config")
public class ApolloPropertiesConfig implements ApplicationContextAware {

    @Autowired
    private  RefreshScope refreshScope;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApolloPropertiesConfig(final RefreshScope refreshScope){
        this.refreshScope = refreshScope;
    }

    @ApolloConfigChangeListener(value = {"application.yml"}, interestedKeyPrefixes = {"tags."})
    private void someChangeHandler(ConfigChangeEvent changeEvent) {
        for (String key : changeEvent.changedKeys()) {
            ConfigChange change = changeEvent.getChange(key);
            log.info(" : {}", change.toString());
        }
        //更新配置
        this.applicationContext.publishEvent(new EnvironmentChangeEvent(changeEvent.changedKeys()));

        refreshScope.refresh("groupTag");
    }
}
