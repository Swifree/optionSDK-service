package com.fota.optionSDKservice;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * @Author huangtao 2019/3/11 8:53 PM
 * @Description 签名测试
 */
public class SignatureDemoTest {
    public static byte[] sha256_HMAC(String message, String secret) {
        byte[] bytes = new byte[256];
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            bytes = sha256_HMAC.doFinal(Base64.encodeBase64(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("Error HmacSHA256 ===========" + e.getMessage());
        }
        return bytes;
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String toSignature(String hexChars) {
        byte[] signature = Base64.encodeBase64(hexChars.getBytes());
        return new String(signature);
    }

    @Test
    public void libraryTest(){
        String secret = "4b30f6407cbc40b183cd143135b75cfe";
        String message = "{\"suid\": \"test\",\"email\": \"fota-test@qq.com\",\"phone\":\"18800000000\",\"name\": \"杭州\"}"+"15523124220001";

        System.out.println("====" + sha256_HMAC(message, secret));
        System.out.println("====" + bytesToHex(sha256_HMAC(message, secret)));
        System.out.println("====" + toSignature(bytesToHex(sha256_HMAC(message, secret))));
    }
}
