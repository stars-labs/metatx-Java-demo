package com.hecochain.metatx.signer;

import org.apache.commons.codec.binary.Base64;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class Util {

    public static String removeHexPrefix(String input) {
        if (StringUtils.isEmpty(input)) {
            return "";
        }

        String sub = "0x";
        int index = input.indexOf(sub);
        if (index < 0) {
            return input;
        } else {
            return input.substring(index + 2);
        }
    }

    public static String addHexPrefix(String input) {
        if (StringUtils.isEmpty(input)){
            return "0x0";
        }
        String sub = "0x";
        int index = input.indexOf(sub);
        if (index < 0) {
            return sub + input;
        } else {
            return input;
        }
    }

    public byte[] decryptAES(String cipherText, String password) throws GeneralSecurityException {
        byte[] cipherBytes = Base64.decodeBase64(cipherText);

        byte[] pwdBytes = pad(password.getBytes());
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

        SecretKeySpec sKeySpec = new SecretKeySpec(pwdBytes, "AES");
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec);

        return cipher.doFinal(cipherBytes);
    }

    public String encryptAES(byte[] plainBytes, String password) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        byte[] keyBytes = new byte[32];
        Arrays.fill(keyBytes, (byte) 0);

        System.arraycopy(password.getBytes(), 0, keyBytes, 0, password.length());

        SecretKeySpec sKeySpec = new SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);

        byte[] cipherBytes = cipher.doFinal(plainBytes);
        return Base64.encodeBase64String(cipherBytes);
    }

    private byte[] pad(byte[] data) {
        int blockSize = 32;
        byte[] padded = new byte[blockSize - data.length % blockSize + data.length];
        System.arraycopy(data, 0, padded, 0, data.length);
        for (int i = data.length; i < padded.length; i++) {
            padded[i] = (byte) 0;
        }

        return padded;
    }
}
