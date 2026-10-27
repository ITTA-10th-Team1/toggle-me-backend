package com.itta.toggleme.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.global.config.JpaAuditingConfig;
import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class LocalAccountRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LocalAccountRepository localAccountRepository;

    @Test
    void 이메일로_자체_계정과_회원을_조회한다() {
        Member member = memberRepository.save(Member.create("user_local0001", "토글러"));
        localAccountRepository.save(LocalAccount.create(member, "local1@example.com", "hash"));

        var found = localAccountRepository.findWithMemberByEmail("local1@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getMember().getHandle()).isEqualTo("user_local0001");
        assertThat(found.get().getPasswordHash()).isEqualTo("hash");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void 없는_이메일은_조회되지_않는다() {
        assertThat(localAccountRepository.findWithMemberByEmail("none@example.com")).isEmpty();
        assertThat(localAccountRepository.existsByEmail("none@example.com")).isFalse();
    }

    @Test
    void 저장된_이메일은_존재_여부를_확인할_수_있다() {
        Member member = memberRepository.save(Member.create("user_local0002", "토글러"));
        localAccountRepository.save(LocalAccount.create(member, "local2@example.com", "hash"));

        assertThat(localAccountRepository.existsByEmail("local2@example.com")).isTrue();
    }

    @Test
    void 같은_이메일은_중복_저장할_수_없다() {
        Member first = memberRepository.save(Member.create("user_local0003", "토글러"));
        Member second = memberRepository.save(Member.create("user_local0004", "토글러"));
        localAccountRepository.saveAndFlush(LocalAccount.create(first, "dup@example.com", "hash"));

        assertThatThrownBy(() -> localAccountRepository.saveAndFlush(
                LocalAccount.create(second, "dup@example.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 한_회원은_자체_계정을_하나만_가질_수_있다() {
        Member member = memberRepository.save(Member.create("user_local0005", "토글러"));
        localAccountRepository.saveAndFlush(LocalAccount.create(member, "one@example.com", "hash"));

        assertThatThrownBy(() -> localAccountRepository.saveAndFlush(
                LocalAccount.create(member, "two@example.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
