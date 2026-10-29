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
import com.itta.toggleme.auth.exception.EmailAlreadyExistsException;
import com.itta.toggleme.auth.exception.LoginIdAlreadyExistsException;
import com.itta.toggleme.global.exception.ErrorCode;
import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import java.sql.SQLException;
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
    private static final String LOGIN_ID_CONFLICT =
            "Duplicate entry 'chaeee22' for key 'local_account.uk_local_account_login_id'";
    private static final String EMAIL_CONFLICT =
            "Duplicate entry 'user@example.com' for key 'local_account.uk_local_account_email'";

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
    void 가입하면_정규화한_아이디와_이메일로_계정을_저장하고_온보딩이_필요한_토큰을_발급한다() {
        Member member = Member.create("user_test0001", "토글러");
        givenAvailableSignUp(member);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.signUp("  Chaeee22 ", "  User@Example.COM ", "password1", "토글러");

        ArgumentCaptor<LocalAccount> captor = ArgumentCaptor.forClass(LocalAccount.class);
        verify(localAccountRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getLoginId()).isEqualTo("chaeee22");
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.onboardingRequired()).isTrue();
    }

    @Test
    void 이미_가입된_아이디면_이메일_확인이나_회원_생성_없이_예외를_던진다() {
        given(localAccountRepository.existsByLoginId("chaeee22")).willReturn(true);

        assertThatThrownBy(() -> localAuthService.signUp("chaeee22", "user@example.com", "password1", "토글러"))
                .isInstanceOf(LoginIdAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ID_ALREADY_EXISTS);

        verify(localAccountRepository, never()).existsByEmail(anyString());
        verify(localAccountRepository, never()).saveAndFlush(any(LocalAccount.class));
        verifyNoInteractions(memberService, passwordEncoder, tokenService);
    }

    @Test
    void 이미_가입된_이메일이면_회원을_만들거나_비밀번호를_해시하지_않고_예외를_던진다() {
        given(localAccountRepository.existsByLoginId("chaeee22")).willReturn(false);
        given(localAccountRepository.existsByEmail("user@example.com")).willReturn(true);

        assertThatThrownBy(() -> localAuthService.signUp("chaeee22", "user@example.com", "password1", "토글러"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(localAccountRepository, never()).saveAndFlush(any(LocalAccount.class));
        verifyNoInteractions(memberService, passwordEncoder, tokenService);
    }

    @Test
    void 중복_확인_이후_같은_아이디가_먼저_저장되면_아이디_중복_예외로_바꾼다() {
        Member member = Member.create("user_test0002", "토글러");
        givenAvailableSignUp(member);
        givenSaveFailsWith(new DataIntegrityViolationException("duplicate", new SQLException(LOGIN_ID_CONFLICT)));

        assertThatThrownBy(() -> localAuthService.signUp("chaeee22", "user@example.com", "password1", "토글러"))
                .isInstanceOf(LoginIdAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ID_ALREADY_EXISTS);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 중복_확인_이후_같은_이메일이_먼저_저장되면_이메일_중복_예외로_바꾼다() {
        Member member = Member.create("user_test0003", "토글러");
        givenAvailableSignUp(member);
        givenSaveFailsWith(new DataIntegrityViolationException("duplicate", new SQLException(EMAIL_CONFLICT)));

        assertThatThrownBy(() -> localAuthService.signUp("chaeee22", "user@example.com", "password1", "토글러"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 어느_제약인지_알_수_없는_중복_오류는_아이디_중복_예외로_처리한다() {
        Member member = Member.create("user_test0004", "토글러");
        givenAvailableSignUp(member);
        givenSaveFailsWith(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> localAuthService.signUp("chaeee22", "user@example.com", "password1", "토글러"))
                .isInstanceOf(LoginIdAlreadyExistsException.class);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 올바른_아이디와_비밀번호로_로그인하면_토큰을_발급한다() {
        Member member = Member.create("user_test0005", "토글러");
        givenAccount(member, "chaeee22", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.login("chaeee22", "password1");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.onboardingRequired()).isTrue();
    }

    @Test
    void 로그인_아이디의_대소문자와_앞뒤_공백은_무시한다() {
        Member member = Member.create("user_test0006", "토글러");
        givenAccount(member, "chaeee22", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.login("  CHAEEE22 ", "password1");

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void 온보딩을_마친_회원은_온보딩이_필요하지_않다() {
        Member member = Member.create("user_test0007", "토글러");
        member.updateProfile("local_done", "토글러");
        member.completeOnboarding();
        givenAccount(member, "chaeee22", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);
        given(tokenService.issue(member)).willReturn(TOKEN);

        LoginResponse response = localAuthService.login("chaeee22", "password1");

        assertThat(response.onboardingRequired()).isFalse();
        assertThat(response.onboardingStep().name()).isEqualTo("DONE");
    }

    @Test
    void 비밀번호가_틀리면_토큰을_발급하지_않고_예외를_던진다() {
        Member member = Member.create("user_test0008", "토글러");
        givenAccount(member, "chaeee22", "hashed");
        given(passwordEncoder.matches("wrong-password", "hashed")).willReturn(false);

        assertThatThrownBy(() -> localAuthService.login("chaeee22", "wrong-password"))
                .isInstanceOf(CredentialsInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIALS_INVALID);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 가입되지_않은_아이디도_해시_비교를_수행하고_같은_예외를_던진다() {
        given(localAccountRepository.findWithMemberByLoginId("nobody")).willReturn(Optional.empty());
        given(passwordEncoder.matches(eq("password1"), anyString())).willReturn(false);

        assertThatThrownBy(() -> localAuthService.login("nobody", "password1"))
                .isInstanceOf(CredentialsInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIALS_INVALID);

        verify(passwordEncoder).matches(eq("password1"), anyString());
        verifyNoInteractions(tokenService);
    }

    @Test
    void 가입되지_않은_아이디에_임시_해시와_일치하는_비밀번호를_보내도_로그인되지_않는다() {
        given(localAccountRepository.findWithMemberByLoginId("nobody")).willReturn(Optional.empty());
        given(passwordEncoder.matches(eq("any-password"), anyString())).willReturn(true);

        assertThatThrownBy(() -> localAuthService.login("nobody", "any-password"))
                .isInstanceOf(CredentialsInvalidException.class);

        verifyNoInteractions(tokenService);
    }

    @Test
    void 탈퇴한_회원은_비밀번호가_맞아도_로그인할_수_없다() {
        Member member = Member.create("user_test0009", "토글러");
        ReflectionTestUtils.setField(member, "status", MemberStatus.WITHDRAWN);
        givenAccount(member, "chaeee22", "hashed");
        given(passwordEncoder.matches("password1", "hashed")).willReturn(true);

        assertThatThrownBy(() -> localAuthService.login("chaeee22", "password1"))
                .isInstanceOf(CredentialsInvalidException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CREDENTIALS_INVALID);

        verifyNoInteractions(tokenService);
    }

    private void givenAvailableSignUp(Member member) {
        given(localAccountRepository.existsByLoginId("chaeee22")).willReturn(false);
        given(localAccountRepository.existsByEmail("user@example.com")).willReturn(false);
        given(memberService.register("토글러")).willReturn(member);
        given(passwordEncoder.encode("password1")).willReturn("hashed");
    }

    private void givenSaveFailsWith(DataIntegrityViolationException exception) {
        willThrow(exception).given(localAccountRepository).saveAndFlush(any(LocalAccount.class));
    }

    private void givenAccount(Member member, String loginId, String passwordHash) {
        given(localAccountRepository.findWithMemberByLoginId(loginId))
                .willReturn(Optional.of(LocalAccount.create(member, loginId, "user@example.com", passwordHash)));
    }
}
