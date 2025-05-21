package com.boot.cms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;

public class Sha256Util {
    private static final Logger logger = LoggerFactory.getLogger(Sha256Util.class);

    public static String encrypt(String text) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            String errorMessage = "SHA-256 암호화 오류: ";
            logger.error(errorMessage, e.getMessage(), e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}
