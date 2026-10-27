package com.itta.toggleme.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalAccountTest {

    @Test
    void 이메일은_앞뒤_공백을_제거하고_소문자로_저장한다() {
        Member member = Member.create("user_test0001", "토글러");

        LocalAccount account = LocalAccount.create(member, "  Toggle.Me@Example.COM  ", "hash");

        assertThat(account.getEmail()).isEqualTo("toggle.me@example.com");
    }

    @Test
    void 대소문자만_다른_이메일은_같은_값으로_정규화된다() {
        assertThat(LocalAccount.normalizeEmail("A@Example.com"))
                .isEqualTo(LocalAccount.normalizeEmail("a@example.COM"));
    }

    @Test
    void 비밀번호_해시와_회원을_그대로_보관한다() {
        Member member = Member.create("user_test0002", "토글러");

        LocalAccount account = LocalAccount.create(member, "a@example.com", "hashed-value");

        assertThat(account.getPasswordHash()).isEqualTo("hashed-value");
        assertThat(account.getMember()).isSameAs(member);
    }
}
