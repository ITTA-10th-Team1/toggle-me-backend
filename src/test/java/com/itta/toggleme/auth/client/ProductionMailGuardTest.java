package com.itta.toggleme.auth.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProductionMailGuardTest {

    private ApplicationContextRunner runner(String... profiles) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles))
                .withUserConfiguration(ProductionMailGuard.class, LogEmailSender.class);
    }

    @Test
    void 운영에서_메일_설정이_없으면_기동에_실패한다() {
        runner("prod").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("MAIL_USERNAME");
        });
    }

    @Test
    void 운영에서_비밀번호만_비어_있어도_기동에_실패한다() {
        runner("prod").withPropertyValues("spring.mail.username=noreply@example.com")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void 운영에서_메일_설정이_있으면_기동하고_로그_발송기는_쓰지_않는다() {
        runner("prod").withPropertyValues("spring.mail.username=noreply@example.com", "spring.mail.password=app-pw")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(LogEmailSender.class);
                });
    }

    @Test
    void 운영이_아니면_메일_설정이_없어도_로그_발송기로_기동한다() {
        runner("local").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LogEmailSender.class);
            assertThat(context).doesNotHaveBean(ProductionMailGuard.class);
        });
    }
}
