package com.itta.toggleme.auth.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * PEM 형식 인증서와 PKCS8 개인 키로 mTLS 클라이언트용 SSLContext를 만든다.
 * 오류 메시지에 파일 내용을 담지 않는다.
 */
final class MtlsSslContextFactory {

    private static final String[] KEY_ALGORITHMS = {"RSA", "EC"};
    // 메모리 안의 KeyStore에만 쓰는 값이라 비밀이 아니다
    private static final char[] IN_MEMORY_PASSWORD = "toss".toCharArray();

    private MtlsSslContextFactory() {
    }

    static SSLContext create(Path certPath, Path keyPath) {
        try {
            Certificate[] chain = readCertificates(certPath);
            PrivateKey privateKey = readPrivateKey(keyPath);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("toss", privateKey, IN_MEMORY_PASSWORD, chain);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, IN_MEMORY_PASSWORD);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            return sslContext;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("토스 mTLS 인증서를 읽을 수 없습니다. 경로와 PEM(PKCS8) 형식을 확인하세요: "
                    + e.getClass().getSimpleName(), e);
        }
    }

    private static Certificate[] readCertificates(Path certPath) throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(certPath)) {
            Collection<? extends Certificate> certificates = CertificateFactory.getInstance("X.509")
                    .generateCertificates(in);
            if (certificates.isEmpty()) {
                throw new IllegalArgumentException("certificate not found");
            }
            return certificates.toArray(new Certificate[0]);
        }
    }

    private static PrivateKey readPrivateKey(Path keyPath) throws IOException, GeneralSecurityException {
        String pem = Files.readString(keyPath);
        if (!pem.contains("BEGIN PRIVATE KEY")) {
            throw new IllegalArgumentException("PKCS8 private key required");
        }
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64));

        for (String algorithm : KEY_ALGORITHMS) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
            } catch (InvalidKeySpecException ignored) {
                // 다음 알고리즘으로 시도한다
            }
        }
        throw new IllegalArgumentException("unsupported private key algorithm");
    }
}
