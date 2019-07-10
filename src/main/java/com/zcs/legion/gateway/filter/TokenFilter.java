package com.zcs.legion.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zcs.legion.gateway.common.ConstantsValues;
import com.zcs.legion.gateway.componet.EncrptGlobalToken;
import com.zcs.legion.gateway.config.GroupTag;
import com.zcs.legion.gateway.filter.exception.InvalidTokenException;
import com.zcsmart.ccks.SE;
import com.zcsmart.ccks.SEFactory;
import com.zcsmart.ccks.codec.Base64;
import com.zcsmart.ccks.enums.CkeysTypeEnum;
import com.zcsmart.ccks.enums.EncTypeEnum;
import com.zcsmart.ccks.exceptions.SecurityLibExecption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.zcs.legion.gateway.filter.exception.InvalidTokenException.*;

/**
 * 
 * @author lance
 * 6/21/2019 17:22
 */
@Slf4j
@Component
public class TokenFilter extends AbstractTokenFilter {

    @Value("${gateway.server.pack}")
    private String sePath;

    @Value("${legion_pack_logs}")
    private String logFilePath;

    @Autowired
    private GroupTag groupTag;

    @Autowired
    RedisTemplate redisTemplate;

    private static SE se = null;

    @Override
    public boolean enable() {
        return true;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void handler(HttpServletRequest request) throws InvalidTokenException {
        log.info("come into tokenFilter ： {}" , request.getRequestURI());

        //验证tag是否存在
        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String groupId = (String)pathVariables.get(ConstantsValues.X_GROUP_ID);
        String tag = StringUtils.substringAfter(request.getRequestURI(), groupId + "/");

        if(groupId.equals(ConstantsValues.X_CHECKE_TAGS) && !checkTag(groupId , tag)){
            throw TAG_CHECKE_ILLEGAL;
        }

        String encToken = request.getHeader(ConstantsValues.X_AUTH_TOKEN);

        //验证token不可为空
        if(StringUtils.isBlank(encToken)){
            throw TOKEN_CHECK_EMPTY;
        }
        //验证token是否过期
        Long expireTime = redisTemplate.getExpire(ConstantsValues.X_PRE_TOKEN + encToken , TimeUnit.MINUTES);
        if(expireTime <= 10 && expireTime >= 0){
            redisTemplate.expire(encToken, 2 , TimeUnit.HOURS);
        }else if(expireTime < 0 ){
            throw TOKEN_EXPIRE_OUT;
        }
        //解密token
        String token = getDecToken(encToken);

        EncrptGlobalToken tokenObj = JSON.parseObject(token , EncrptGlobalToken.class);

        log.info("enc token data : {}" , JSONObject.toJSONString(tokenObj));
        request.setAttribute(ConstantsValues.X_ACCOUNT , tokenObj.getAccount());
        request.setAttribute(ConstantsValues.X_BUSINESS_BRH_ID , tokenObj.getPlatBrhMap()
                .get(groupTag.getGroupIdAndPlatCodes().get(groupId)));
    }

    /**
     * token解密
     * @param encToken
     * @return
     * @throws IOException
     * @throws SecurityLibExecption
     */
    private String getDecToken(String encToken){
        try {
            se = SEFactory.init(sePath, null, logFilePath);
            byte[] bytes = se.decData(0, Base64.decodeBase64(encToken),  CkeysTypeEnum.CKEYS80,
                    EncTypeEnum.AES_192_CBC);
            return new String(bytes);
        } catch (Exception e) {
            log.error(e.getMessage() , e);
            throw SE_DEC_TOKEN_EXCEPTION;
        }
    }

    /**
     * 验证tag是否存在
     * @param tag
     * @return
     */
    private boolean checkTag(String groupId , String tag) {
        List<String> tagsList = groupTag.getTokenTags().get(groupId);
        if(!CollectionUtils.isEmpty(tagsList) && tagsList.contains(tag)){
            return true;
        }
        return false;
    }
}
