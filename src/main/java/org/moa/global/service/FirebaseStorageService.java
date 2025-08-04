package org.moa.global.service;

import com.google.cloud.storage.*;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FirebaseStorageService {

    @Value("${firebase.bucket-name}")
    private String bucketName;

    /** 파일을 Firebase Storage에 업로드하고, 저장된 파일 이름 반환 **/
    public String uploadAndGetFileName(MultipartFile file) throws IOException {
        // 빈 파일 업로드 방지
        if (file == null || file.isEmpty()) {
            return null;
        }

        Bucket bucket = StorageClient.getInstance().bucket(bucketName);

        String originalFileName = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFileName);

        // 파일 이름 중복을 피하기 위해 UUID 사용
        String storedFileName = String.format("%s%s", UUID.randomUUID(), (extension != null ? "." + extension : ""));

        // Content-Type 결정
        String contentType = determineContentType(file);

        // Firebase Storage에 파일 업로드
        bucket.create(storedFileName, file.getBytes(), contentType);

        log.info("파일이 성공적으로 업로드 되었습니다: {} (Content-Type: {})", storedFileName, contentType);

        // 저장된 파일 이름 반환
        return storedFileName;
    }


    /** 파일 이름을 받아, 해당 파일에 접근할 수 있는 임시 서명된 URL을 생성 **/
    public String getSignedUrl(String fileName) {
        // 파일 이름이 유효하지 않으면 즉시 null 반환
        if (!StringUtils.hasText(fileName)) {
            return null;
        }

        try {
            Bucket bucket = StorageClient.getInstance().bucket(bucketName);
            Blob blob = bucket.get(fileName);

            if (blob == null) {
                log.warn("파일을 찾을 수 없습니다. : {}", fileName);
                return null; // 파일을 찾을 수 없으면 null 반환
            }
            // V4 서명은 최대 7일, 여기서는 3시간으로 짧게 설정하여 보안 강화
            return blob.signUrl(3, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature()).toString();
        } catch (Exception e) {
            // StorageException 등 예상치 못한 에러 발생 시 로그를 남기고 null 반환
            log.error("파일 URL을 생성하는 중 오류 발생: {}", fileName, e);
            return null;
        }
    }


    /** 파일 Content-Type 결정 **/
    private String determineContentType(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String originalContentType = file.getContentType();

        // .jfif와 같이 잘 알려지지 않은 확장자를 위해 직접 매핑
        if (extension != null) {
            switch (extension.toLowerCase()) {
                case "jpeg":
                case "jpg":
                case "jfif": // jfif를 jpeg로 명시적으로 처리
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "webp":
                    return "image/webp";
                case "gif":
                    return "image/gif";
            }
        }

        // 매핑되는 확장자가 없으면 클라이언트가 보낸 원본 Content-Type을 사용
        return originalContentType;
    }


    /** Firebase Storage에서 파일 삭제 **/
    public void deleteFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return;
        }

        try {
            Bucket bucket = StorageClient.getInstance().bucket(bucketName);
            Blob blob = bucket.get(fileName);

            if (blob == null) {
                log.warn("삭제하려는 파일이 Storage에 존재하지 않습니다: {}", fileName);
                return;
            }
            blob.delete();
            log.info("파일이 성공적으로 삭제되었습니다: {}", fileName);
        } catch (Exception e) {
            log.error("파일 삭제 중 오류가 발생했습니다: {}", fileName, e);
        }
    }
}