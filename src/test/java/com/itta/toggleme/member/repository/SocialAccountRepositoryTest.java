package com.itta.toggleme.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itta.toggleme.global.config.JpaAuditingConfig;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.SocialAccount;
import com.itta.toggleme.member.domain.SocialProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class SocialAccountRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Test
    void 제공자와_제공자_회원번호로_소셜_계정과_회원을_조회한다() {
        Member member = memberRepository.save(Member.create("user_test0001", "토글러"));
        socialAccountRepository.save(SocialAccount.connect(member, SocialProvider.KAKAO, "1234567890"));

        Optional<SocialAccount> found = socialAccountRepository.findWithMember(SocialProvider.KAKAO, "1234567890");

        assertThat(found).isPresent();
        assertThat(found.get().getMember().getHandle()).isEqualTo("user_test0001");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void 같은_제공자_회원번호는_중복_저장할_수_없다() {
        Member member = memberRepository.save(Member.create("user_test0002", "토글러"));
        socialAccountRepository.saveAndFlush(SocialAccount.connect(member, SocialProvider.KAKAO, "1111"));

        assertThatThrownBy(() -> socialAccountRepository.saveAndFlush(
                SocialAccount.connect(member, SocialProvider.KAKAO, "1111")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
