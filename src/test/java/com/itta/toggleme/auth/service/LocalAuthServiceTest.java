package com.itta.toggleme.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.dto.TokenResponse;
import com.itta.toggleme.auth.exception.CredentialsInvalidException;
import com.itta.toggleme.auth.exception.LoginIdAlreadyExistsException;
import com.itta.toggleme.global.exception.ErrorCode;
import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    private static final TokenResponse TOKEN = TokenResponse.of("access-token", "refresh-token", 1800L);

    @Mock
    private LocalAccountRepository localAccountRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LocalAuthService localAuthService;

    @Test
    void 가입하면_해시된_자체_계정과_온보딩_토큰을_발급한다() {
        Member member = Member.create("user_test0001", "토글러");
        given(localAccountRepository.existsByEmail("user@example.com")).willReturn(false);
        given(memberService.register("토글러")).willReturn(member);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.signUp("  User@Example.COM ", "password1", "토글러");

        ArgumentCaptor<LocalAccount> captor = ArgumentCaptor.forClass(LocalAccount.class);
        verify(localAccountRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.onboardingRequired()).isTrue();
    }

    @Test
    void 이미_가입된_이메일이면_회원_생성과_비밀번호_해시_없이_예외를_던진다() {
        given(localAccountRepository.existsByEmail("user@example.com")).willReturn(true);

        assertThatThrownBy(() -> localAuthService.signUp("user@example.com", "password1", "토글러"))
                .isInstanceOf(LoginIdAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ID_ALREADY_EXISTS);

        verifyNoInteractions(memberService, passwordEncoder, tokenService);
        verify(localAccountRepository, never()).saveAndFlush(any(LocalAccount.class));
    }

    @Test
    void 중복_확인_이후_같은_이메일이_먼저_저장되면_이메일_중복_예외로_바꾼다() {
        Member member = Member.create("user_test0002", "토글러");
        given(localAccountRepository.existsByEmail("user@example.com")).willReturn(false);
        given(memberService.register("토글러")).willReturn(member);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(localAccountRepository).saveAndFlush(any(LocalAccount.class));

        assertThatThrownBy(() -> localAuthService.signUp("user@example.com", "password1", "토글러"))
                .isInstanceOf(LoginIdAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ID_ALREADY_EXISTS);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 올바른_이메일과_비밀번호로_로그인하면_토큰을_발급한다() {
        Member member = Member.create("user_test0003", "토글러");
        givenAccount(member, "user@example.com", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.login("user@example.com", "password1");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.onboardingRequired()).isTrue();
    }

    @Test
    void 로그인_이메일의_대소문자와_앞뒤_공백은_무시한다() {
        Member member = Member.create("user_test0004", "토글러");
        givenAccount(member, "user@example.com", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.login("  USER@Example.com ", "password1");

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void 온보딩을_마친_회원은_온보딩이_필요하지_않다() {
        Member member = Member.create("user_test0005", "토글러");
        member.updateProfile("local_done", "토글러");
        member.completeOnboarding();
        givenAccount(member, "user@example.com", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.login("user@example.com", "password1");

        assertThat(response.onboardingRequired()).isFalse();
        assertThat(response.onboardingStep().name()).isEqualTo("DONE");
    }

    @Test
    void 비밀번호가_틀리면_토큰을_발급하지_않고_예외를_던진다() {
        Member member = Member.create("user_test0006", "토글러");
        givenAccount(member, "user@example.com", "hashed");
        given(passwordEncoder.matches("wrong-password", "hashed")).willReturn(false);

        assertThatThrownBy(() -> localAuthService.login("user@example.com", "wrong-password"))
                .isInstanceOf(CredentialsInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIALS_INVALID);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 가입되지_않은_이메일도_해시_비교를_수행하고_같은_예외를_던진다() {
        given(localAccountRepository.findWithMemberByEmail("none@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.matches(eq("password1"), anyString())).willReturn(false);

        assertThatThrownBy(() -> localAuthService.login("none@example.com", "password1"))
                .isInstanceOf(CredentialsInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIALS_INVALID);

        verify(passwordEncoder).matches(eq("password1"), anyString());
        verifyNoInteractions(tokenService);
    }

    @Test
    void 미가입_이메일은_임시_해시와_같은_비밀번호로도_로그인할_수_없다() {
        given(localAccountRepository.findWithMemberByEmail("none@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.matches(eq("any-password"), anyString())).willReturn(true);

        assertThatThrownBy(() -> localAuthService.login("none@example.com", "any-password"))
                .isInstanceOf(CredentialsInvalidException.class);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 탈퇴한_회원은_비밀번호가_맞아도_로그인할_수_없다() {
        Member member = Member.create("user_test0007", "토글러");
        ReflectionTestUtils.setField(member, "status", MemberStatus.WITHDRAWN);
        givenAccount(member, "user@example.com", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);

        assertThatThrownBy(() -> localAuthService.login("user@example.com", "password1"))
                .isInstanceOf(CredentialsInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIALS_INVALID);

        verifyNoInteractions(tokenService);
    }

    private void givenAccount(Member member, String email, String passwordHash) {
        given(localAccountRepository.findWithMemberByEmail(email))
                .willReturn(Optional.of(LocalAccount.create(member, email, passwordHash)));
    }
}
