package org.moa.global.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// 문자열을 QR 코드 이미지로 만들어주는 유틸 클래스
@Slf4j
public class QrCodeUtil {

    // QR 생성 + 암호화
    public static String generateEncryptedQr(String plainTextJson) throws Exception {
        String encrypted = AesUtil.encryptWithIv(plainTextJson); // AesUtil.encryptWithIv : 입력받은 평문 JSON을 암호화 -> encrypted 문자열(Base64암호문 + IV)로 변환
        BufferedImage qrImage = generateQrImage(encrypted); // 암호화된 문자열을 QR 이미지로 변환

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(qrImage, "png", baos); // QR 이미지를 메모리 스트림(baos)에 PNG 형식으로 저장

        byte[] imageBytes = baos.toByteArray(); // 메모리에 저장된 이미지 데이터를 바이트 배열로 추출

        return Base64.getEncoder().encodeToString(imageBytes); // 바이트 배열을 Base64 문자열로 인코딩하여 반환 (HTML <img>에 쓰기 위해서)
    }

    // QR 코드 생성
    private static BufferedImage generateQrImage(String data) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200); // QR 사이즈 200 X 200
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
