package com.zcs.legion.gateway.config;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.protobuf.Message;
import com.legion.client.api.sample.T;

/**
 * 定义常量
 * @author lance
 * @since 2019.2.23 15:38
 */
public class Constants {
    /***
     * <GroupID+Tag, RequestMessage, ReplyMessage>
     */
    public static Table<String, Class<?extends Message>, Class<? extends Message>> table = HashBasedTable.create();
    static {
        table.put("accountAdd", T.SampleMessage.class, T.SampleMessage.class);
        table.put("accountDelete", T.SampleMessage.class, T.SampleMessage.class);
        table.put("accountDetail", T.TMessage.class, T.TMessage.class);
        table.put("accountQuery", T.SampleMessage.class, T.SampleMessage.class);
    }
}
