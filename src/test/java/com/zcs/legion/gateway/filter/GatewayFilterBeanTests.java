package com.zcs.legion.gateway.filter;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class GatewayFilterBeanTests {
   private List<AbstractTokenFilter> lists = Lists.newArrayList();

   @Before
   public void init(){
       lists.add(new TokenFilter());
   }

    @Test
    public void handler(){
        lists.stream().filter(AbstractTokenFilter::enable).sorted(Comparator.comparing(AbstractTokenFilter::order))
          .forEach(o -> {
            o.handler(null);
        });
    }
}
