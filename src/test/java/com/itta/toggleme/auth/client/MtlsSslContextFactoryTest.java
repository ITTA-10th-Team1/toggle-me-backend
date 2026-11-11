package com.itta.toggleme.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

class MtlsSslContextFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void 파일이_없으면_경로만_알려_주는_예외를_던진다() {
        Path missing = tempDir.resolve("none.crt");

        assertThatThrownBy(() -> MtlsSslContextFactory.create(missing, tempDir.resolve("none.key")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("토스 mTLS 인증서를 읽을 수 없습니다");
    }

    @Test
    void PKCS8이_아닌_키는_거절하고_내용을_메시지에_담지_않는다() throws Exception {
        Path cert = tempDir.resolve("a.crt");
        Path key = tempDir.resolve("a.key");
        Files.writeString(cert, "not a cert");
        Files.writeString(key, "-----BEGIN RSA PRIVATE KEY-----\nSECRET-BODY\n-----END RSA PRIVATE KEY-----");

        assertThatThrownBy(() -> MtlsSslContextFactory.create(cert, key))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("SECRET-BODY");
    }

    // 실제 인증서가 있는 로컬에서만 실행된다. CI에서는 환경 변수가 없어 건너뛴다
    @Test
    @EnabledIfEnvironmentVariable(named = "TOSS_MTLS_CERT_PATH", matches = ".+")
    void 실제_인증서와_개인_키로_SSLContext를_만든다() {
        Path cert = Path.of(System.getenv("TOSS_MTLS_CERT_PATH"));
        Path key = Path.of(System.getenv("TOSS_MTLS_KEY_PATH"));

        assertThat(MtlsSslContextFactory.create(cert, key)).isNotNull();
    }
}
