package com.zcs.legion.gateway.componet;

import lombok.Data;

import java.util.Map;

/**
 * @author ：lyj
 * @date ：2019/6/26 10:55
 * @description：
 */
@Data
public class EncrptGlobalToken {
    // 用户id
    int userId;
    // 用户账号
    String account;
    // 平台和机构信息
    Map<String, String> platBrhMap;
    // 时间戳
    String timestamp;
}
