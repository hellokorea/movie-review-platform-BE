package com.cookie.global.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AWSS3Service {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "svg"); // 지원하는 확장자
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public String uploadImage(MultipartFile image) {
        validateImageFile(image);

        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        String s3Filename = UUID.randomUUID().toString().substring(0, 10) + "_" + originalFilename;

        InputStream is = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            is = image.getInputStream();
            byte[] bytes = IOUtils.toByteArray(is);

            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setContentLength(bytes.length);

            if ("svg".equals(extension)) {
                metaData.setContentType("image/svg+xml");
            } else {
                metaData.setContentType("image/" + extension);
            }

            byteArrayInputStream = new ByteArrayInputStream(bytes);

            // S3에 업로드
            PutObjectRequest putObjectRequest
                    = new PutObjectRequest(bucketName, s3Filename, byteArrayInputStream, metaData)
                    .withCannedAcl(CannedAccessControlList.PublicRead);
            amazonS3.putObject(putObjectRequest);
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            try {
                byteArrayInputStream.close();
                is.close();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }

        return amazonS3.getUrl(bucketName, s3Filename).toString();
    }

    /**
     * S3에서 이미지 삭제
     */
    public void deleteImage(String filename) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, filename));
            log.info("이미지 삭제 완료: {}", filename);

        } catch (Exception e) {
            log.error("S3에서 이미지를 삭제하는 중 오류가 발생했습니다. 파일명: {}", filename, e);
            throw new RuntimeException("S3에서 이미지를 삭제하는 중 오류가 발생했습니다.", e);
        }
    }


    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 이름에 확장자가 포함되어 있지 않습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + extension);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과할 수 없습니다.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("잘못된 MIME 타입입니다: " + mimeType);
        }
    }
}
