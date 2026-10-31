package com.itta.toggleme.image.service;

import com.itta.toggleme.image.config.S3Properties;
import com.itta.toggleme.image.dto.ImageUploadUrlResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);
    private static final Duration VIEW_URL_TTL = Duration.ofMinutes(30);
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/heic", "heic"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public ImageUploadUrlResponse createUploadUrl(Long memberId, String contentType, long contentLength) {
        String key = keyPrefix(memberId) + UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(request -> request
                .signatureDuration(UPLOAD_URL_TTL)
                .putObjectRequest(object -> object
                        .bucket(properties.bucket())
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)));
        return new ImageUploadUrlResponse(
                presigned.url().toString(),
                key,
                Map.of("Content-Type", contentType, "Content-Length", String.valueOf(contentLength)),
                presigned.expiration()
        );
    }

    public boolean isUploadedBy(Long memberId, String key) {
        if (!key.startsWith(keyPrefix(memberId))) {
            return false;
        }
        try {
            s3Client.headObject(request -> request.bucket(properties.bucket()).key(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public String createViewUrl(String key) {
        return s3Presigner.presignGetObject(request -> request
                        .signatureDuration(VIEW_URL_TTL)
                        .getObjectRequest(object -> object.bucket(properties.bucket()).key(key)))
                .url()
                .toString();
    }

    public ResponseBytes<GetObjectResponse> download(String key) {
        return s3Client.getObjectAsBytes(request -> request.bucket(properties.bucket()).key(key));
    }

    public void delete(String key) {
        s3Client.deleteObject(request -> request.bucket(properties.bucket()).key(key));
    }

    private String keyPrefix(Long memberId) {
        return "images/" + memberId + "/";
    }
}
