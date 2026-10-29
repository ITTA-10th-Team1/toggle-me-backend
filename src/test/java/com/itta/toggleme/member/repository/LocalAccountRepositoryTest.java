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
    void 아이디로_자체_계정과_회원을_조회한다() {
        Member member = memberRepository.save(Member.create("user_local0001", "토글러"));
        localAccountRepository.save(LocalAccount.create(member, "local_one", "local1@example.com", "hash"));

        var found = localAccountRepository.findWithMemberByLoginId("local_one");

        assertThat(found).isPresent();
        assertThat(found.get().getMember().getHandle()).isEqualTo("user_local0001");
        assertThat(found.get().getEmail()).isEqualTo("local1@example.com");
        assertThat(found.get().getPasswordHash()).isEqualTo("hash");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void 이메일로는_계정을_조회할_수_없다() {
        Member member = memberRepository.save(Member.create("user_local0002", "토글러"));
        localAccountRepository.save(LocalAccount.create(member, "local_two", "local2@example.com", "hash"));

        assertThat(localAccountRepository.findWithMemberByLoginId("local2@example.com")).isEmpty();
    }

    @Test
    void 없는_아이디와_이메일은_조회되지_않는다() {
        assertThat(localAccountRepository.findWithMemberByLoginId("nobody")).isEmpty();
        assertThat(localAccountRepository.existsByLoginId("nobody")).isFalse();
        assertThat(localAccountRepository.existsByEmail("none@example.com")).isFalse();
    }

    @Test
    void 저장된_아이디와_이메일은_존재_여부를_확인할_수_있다() {
        Member member = memberRepository.save(Member.create("user_local0003", "토글러"));
        localAccountRepository.save(LocalAccount.create(member, "local_three", "local3@example.com", "hash"));

        assertThat(localAccountRepository.existsByLoginId("local_three")).isTrue();
        assertThat(localAccountRepository.existsByEmail("local3@example.com")).isTrue();
    }

    @Test
    void 같은_아이디는_중복_저장할_수_없고_오류에_아이디_제약_이름이_들어_있다() {
        Member first = memberRepository.save(Member.create("user_local0004", "토글러"));
        Member second = memberRepository.save(Member.create("user_local0005", "토글러"));
        localAccountRepository.saveAndFlush(LocalAccount.create(first, "dup_login", "dup1@example.com", "hash"));

        assertThatThrownBy(() -> localAccountRepository.saveAndFlush(
                LocalAccount.create(second, "dup_login", "dup2@example.com", "hash")))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, e ->
                        assertThat(e.getMostSpecificCause().getMessage()).contains("uk_local_account_login_id"));
    }

    @Test
    void 같은_이메일은_중복_저장할_수_없고_오류에_이메일_제약_이름이_들어_있다() {
        Member first = memberRepository.save(Member.create("user_local0006", "토글러"));
        Member second = memberRepository.save(Member.create("user_local0007", "토글러"));
        localAccountRepository.saveAndFlush(LocalAccount.create(first, "dup_one", "dup@example.com", "hash"));

        assertThatThrownBy(() -> localAccountRepository.saveAndFlush(
                LocalAccount.create(second, "dup_two", "dup@example.com", "hash")))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, e ->
                        assertThat(e.getMostSpecificCause().getMessage()).contains("uk_local_account_email"));
    }

    @Test
    void 한_회원은_자체_계정을_하나만_가질_수_있다() {
        Member member = memberRepository.save(Member.create("user_local0008", "토글러"));
        localAccountRepository.saveAndFlush(LocalAccount.create(member, "one_account", "one@example.com", "hash"));

        assertThatThrownBy(() -> localAccountRepository.saveAndFlush(
                LocalAccount.create(member, "two_account", "two@example.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
