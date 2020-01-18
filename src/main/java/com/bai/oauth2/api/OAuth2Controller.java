package com.bai.oauth2.api;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.bai.oauth2.common.BaseResponse;
import com.bai.oauth2.common.GrantTypeEnum;
import com.bai.oauth2.common.HttpStatusMsg;
import com.bai.oauth2.common.PropertyService;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.Version;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @Description: 端点访问控制包装类 示例：https://www.programcreek.com/java-api-examples/?code=h819/spring-boot/spring-boot-master/spring-security-oauth/spring-security-oauth2-client/src/main/java/com/base/oauth2/client/controller/SpringOauth2ClientController.java#
 * @ProjectName: spring-parent
 * @Package: com.yaomy.security.oauth2.api.OAuth2Controller
 * @Date: 2019/7/22 15:57
 * @Version: 1.0
 */
@SuppressWarnings("all")
@RestController
@RequestMapping(value = "oauth2")
public class OAuth2Controller {

    @Autowired
    @Lazy
    private TokenStore tokenStore;

    @Autowired
    private PropertyService propertyService;

    /**
     * @Description /oauth/token(令牌端点) 获取用户token信息
     * @Date 2019/7/22 15:59
     * @Version  1.0
     */
    @PostMapping(value = "token")
    public ResponseEntity<BaseResponse> getToken(@RequestParam String username, @RequestParam String password){

        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setId("resource_password_id");
        resource.setClientId("clientId");
        resource.setClientSecret("baiSecret");
        resource.setGrantType("passoword");
        //resource.setAccessTokenUri("http://localhost:8080/auth_user/get_auth_code");
        resource.setAccessTokenUri("http://localhost:8080/oauth/token");
        resource.setUsername(username);
        resource.setPassword(password);
        resource.setScope(Arrays.asList("all"));

        OAuth2RestTemplate template = new OAuth2RestTemplate(resource);
        ResourceOwnerPasswordAccessTokenProvider provider = new ResourceOwnerPasswordAccessTokenProvider();
        template.setAccessTokenProvider(provider);
        try {
            OAuth2AccessToken accessToken = template.getAccessToken();
            Map<String, Object> result = new HashMap();
            result.put("access_token", accessToken.getValue());
            result.put("token_type", accessToken.getTokenType());
            result.put("refresh_token", accessToken.getRefreshToken().getValue());
            result.put("expires_in", accessToken.getExpiresIn());
            result.put("scope",  StringUtils.join(accessToken.getScope(), ","));
            result.putAll(accessToken.getAdditionalInformation());

            Collection<? extends GrantedAuthority> authorities = tokenStore.readAuthentication(template.getAccessToken()).getUserAuthentication().getAuthorities();
            List<Map> list = new ArrayList();
            for(GrantedAuthority authority:authorities){
                list.add(JSONObject.parseObject(authority.getAuthority()));

            }
            result.put("authorities", list);

            return BaseResponse.createResponseEntity(HttpStatusMsg.OK, result);
        } catch (Exception e){
            e.printStackTrace();
            return BaseResponse.createResponseEntity(HttpStatusMsg.AUTHENTICATION_EXCEPTION);
        }
    }
    /**
     * @Description /oauth/token（令牌端点）刷新token信息
     * @Date 2019/7/25 16:13
     * @Version  1.0
     */
    @PostMapping(value = "refresh_token")
    public ResponseEntity<BaseResponse> refreshToken(@RequestParam String refresh_token){
        try {
            ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
            resource.setId(propertyService.getProperty("spring.security.oauth.resource.id"));
            resource.setClientId(propertyService.getProperty("spring.security.oauth.resource.client.id"));
            resource.setClientSecret(propertyService.getProperty("spring.security.oauth.resource.client.secret"));
            resource.setGrantType(GrantTypeEnum.REFRESH_TOKEN.getGrant_type());
            resource.setAccessTokenUri(propertyService.getProperty("spring.security.oauth.token.uri"));

            ResourceOwnerPasswordAccessTokenProvider provider = new ResourceOwnerPasswordAccessTokenProvider();
            OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(refresh_token);
            OAuth2AccessToken accessToken = provider.refreshAccessToken(resource, refreshToken, new DefaultAccessTokenRequest());

            Map<String, Object> result = new LinkedHashMap();
            result.put("access_token", accessToken.getValue());
            result.put("token_type", accessToken.getTokenType());
            result.put("refresh_token", accessToken.getRefreshToken().getValue());
            result.put("expires_in", accessToken.getExpiresIn());
            result.put("scope", StringUtils.join(accessToken.getScope(), ","));
            result.putAll(accessToken.getAdditionalInformation());

            Collection<? extends GrantedAuthority> authorities = tokenStore.readAuthentication(accessToken).getUserAuthentication().getAuthorities();
            List<Map> list = new ArrayList();
            for(GrantedAuthority authority:authorities){
                list.add(JSONObject.parseObject(authority.getAuthority()));
            }
            result.put("authorities", list);

            return BaseResponse.createResponseEntity(HttpStatusMsg.OK, result);
        } catch (Exception e){
            e.printStackTrace();
            return BaseResponse.createResponseEntity(HttpStatusMsg.AUTHENTICATION_EXCEPTION);
        }
    }
    /**
     * @Description oauth/check_token（端点校验）token有效性
     * @Date 2019/7/25 16:22
     * @Version  1.0
     */
    @PostMapping(value = "check_token")
    public ResponseEntity<BaseResponse> checkToken(@RequestParam String access_token){
        try {
            OAuth2AccessToken accessToken = tokenStore.readAccessToken(access_token);
            OAuth2Authentication auth2Authentication = tokenStore.readAuthentication(access_token);
            Map<String, Object> map = new HashMap();
            //用户名
            map.put("username", auth2Authentication.getUserAuthentication().getName());
            //是否过期
            map.put("isExpired", accessToken.isExpired());
            //过期时间
            //map.put("expiration", DateFormatUtils.format(accessToken.getExpiration(), DateFormatEnum.YYYY_MM_DD_HH_MM_SS.getFormat()));
            return BaseResponse.createResponseEntity(HttpStatusMsg.OK, map);
        } catch (Exception e){
            e.printStackTrace();
            return BaseResponse.createResponseEntity(HttpStatusMsg.AUTHENTICATION_EXCEPTION);
        }
    }
    /**
     * @Description 账号退出
     * @Date 2019/7/25 17:47
     * @Version  1.0
     */
    @PostMapping(value = "logout")
    public ResponseEntity<BaseResponse> logOut(@RequestParam String access_token){
        try {
            if(!StringUtils.isEmpty(access_token)){
                OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(access_token);
                if(oAuth2AccessToken != null){
                    System.out.println("----access_token是："+oAuth2AccessToken.getValue());
                    tokenStore.removeAccessToken(oAuth2AccessToken);
                    OAuth2RefreshToken oAuth2RefreshToken = oAuth2AccessToken.getRefreshToken();
                    tokenStore.removeRefreshToken(oAuth2RefreshToken);
                    tokenStore.removeAccessTokenUsingRefreshToken(oAuth2RefreshToken);
                }
            }
            return BaseResponse.createResponseEntity(HttpStatusMsg.OK);
        } catch (Exception e){
            return BaseResponse.createResponseEntity(HttpStatusMsg.LOGOUT_EXCEPTION);
        }
    }
}