package org.moa.global.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

// AES 암호화/복호화를 위한 유틸 클래스
public class AesUtil {

    private static String SECRET_KEY;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding"; // AES는 대칭키 알고리즘, CBC는 블록 암호화 방식, PKCS5Padding은 데이터 길이를 블록 단위로 맞춤
    private static final String CHARSET = "UTF-8";

    public static void setSecretKey(String key) {
        SECRET_KEY = key;
    }

    // 암호화
    public static String encryptWithIv(String plainText) {
        try {
            byte[] keyBytes = SECRET_KEY.getBytes(CHARSET);
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, "AES"); // 비밀 키 객체(skeySpec) 생성

            // IV 생성 (16바이트)
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv); // 랜덤 IV 생성
            IvParameterSpec ivSpec = new IvParameterSpec(iv); // 암호화 설정에 쓸 수 있도록 래핑

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec); // Cipher.ENCRYPT_MODE = 암호화 모드로 설정, 키와 IV를 설정
            byte[] encrypted = cipher.doFinal(plainText.getBytes(CHARSET)); // 평문을 바이트로 바꿔서 암호화 실행 -> 암호화된 바이트 배열

            // encryptedWithIv = [IV][암호문]
            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            // iv 배열을 0부터 복사 (0-15번 인덱스)
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);
            // encrypted 배열을 16번 인덱스부터 복사

            return Base64.getUrlEncoder().encodeToString(encryptedWithIv); // [IV][암호문]을 Base64로 인코딩해서 문자열로 반환 URL-safe encoding

        } catch (Exception e) {
            throw new RuntimeException("암호화 실패", e);
        }
    }

    // 복호화
    public static String decryptWithIv(String encryptedText) {
        try {
            byte[] encryptedWithIv = Base64.getUrlDecoder().decode(encryptedText); // Base64 인코딩된 문자열을 바이트 배열로 디코딩 URL-safe decoding

            byte[] iv = new byte[16];
            byte[] encrypted = new byte[encryptedWithIv.length - 16]; // [IV][암호문]
            System.arraycopy(encryptedWithIv, 0, iv, 0, 16); // 앞의 16바이트는 IV,
            System.arraycopy(encryptedWithIv, 16, encrypted, 0, encrypted.length); // 나머지는 암호문으로 분리

            IvParameterSpec ivSpec = new IvParameterSpec(iv); // 복호화를 위한 IV
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(CHARSET), "AES"); // 복호화를 위한 키

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec); // Cipher.DECRYPT_MODE = 복호화 모드로 설정
            byte[] original = cipher.doFinal(encrypted); // Base64로 인코딩된 문자열을 다시 암호화된 바이트 배열로 복호화 실행 -> 원래의 평문 바이트 배열

            return new String(original, CHARSET); // 바이트 배열을 다시 문자열로 복원

        } catch (Exception e) {
            throw new RuntimeException("복호화 실패", e);
        }
    }
}
