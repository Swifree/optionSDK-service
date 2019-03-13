package com.fota.optionSDKservice;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

/**
 * @Author huangtao 2019/3/13 8:10 PM
 * @Description 调试demo
 */
public class CustomerDebugDemoTest {
    //预发
    final String url = "https://api-test.fota.com/mapi/v1";
    final String key = "fota";
    final String secret = "fota";

    @Test
    public void addTest() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        //body json
        String requestBody = "{\"suid\": \"test\",\"email\": \"fota-test@qq.com\",\"phone\":\"18800000000\",\"name\": \"杭州\"}";
        String sign = requestBody + timestamp;
        String sign_utf8 = new String(sign.getBytes(StandardCharsets.UTF_8));
        String sign_utf8_base64 = Base64.encodeBase64String(sign_utf8.getBytes());
        //加密
        String sign_utf8_base64_hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmacHex(sign_utf8_base64);
        String signature = Base64.encodeBase64String(sign_utf8_base64_hmac.getBytes());
        JSONObject jsonObject = postReturn(requestBody, timestamp, signature, "/users/subaccount/add");
        assert String.valueOf(jsonObject.get("code")).equals("0");
    }

    /**
     * post
     * @param requestBody
     * @param timestamp
     * @param signature
     */
    private JSONObject postReturn(String requestBody, String timestamp, String signature, String postUrl) {
        //headers
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("apikey", key);
        requestHeaders.add("timestamp", timestamp);
        requestHeaders.add("signature", signature);
        requestHeaders.add("content-type", "application/json;charset=utf-8");
        requestHeaders.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");

        //HttpEntity
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, requestHeaders);
        RestTemplate restTemplate = new RestTemplate();
        //post
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url+postUrl, requestEntity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        return jsonObject;
    }

    private JSONObject getRetuen(String timestamp, String signature, String postUrl) {
        //headers
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("apikey", key);
        requestHeaders.add("timestamp", timestamp);
        requestHeaders.add("signature", signature);
        requestHeaders.add("content-type", "application/json;charset=utf-8");
        requestHeaders.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");

        //HttpEntity
        HttpEntity<String> requestEntity = new HttpEntity<>(null, requestHeaders);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(url+postUrl, HttpMethod.GET, requestEntity, String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        return jsonObject;
    }
}
