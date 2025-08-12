package org.moa.global.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

// 문자열을 QR 코드 이미지로 만들어주는 유틸 클래스
@Slf4j
public class QrCodeUtil {

    // QR 생성
    public static String generateEncryptedQr(String encryptedData) throws Exception {
        BufferedImage qrImage = generateQrImage(encryptedData);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(qrImage, "png", baos); // QR 이미지를 메모리 스트림(baos)에 PNG 형식으로 저장

        byte[] imageBytes = baos.toByteArray(); // 메모리에 저장된 이미지 데이터를 바이트 배열로 추출

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // QR 코드 생성
    private static BufferedImage generateQrImage(String data) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
