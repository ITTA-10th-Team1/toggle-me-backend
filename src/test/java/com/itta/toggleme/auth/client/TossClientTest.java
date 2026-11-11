package com.itta.toggleme.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.itta.toggleme.auth.exception.TossApiException;
import com.itta.toggleme.auth.exception.TossAuthorizationCodeInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TossClientTest {

    private static final String BASE_URL = "https://toss.test";
    private static final String TOKEN_URL = BASE_URL + "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";
    private static final String ME_URL = BASE_URL + "/api-partner/v1/apps-in-toss/user/oauth2/login-me";

    private MockRestServiceServer server;
    private TossClient tossClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        tossClient = new TossClient(builder.build());
    }

    @Test
    void 인가_코드로_토큰을_받아_userKey를_조회한다() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.authorizationCode").value("code-1"))
                .andExpect(jsonPath("$.referrer").value("SANDBOX"))
                .andRespond(withSuccess("{\"accessToken\":\"toss-access\",\"refreshToken\":\"r\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(ME_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer toss-access"))
                .andRespond(withSuccess("{\"userKey\":443731104,\"scope\":[\"user_key\"]}",
                        MediaType.APPLICATION_JSON));

        TossUserInfo userInfo = tossClient.getUserInfo("code-1", "SANDBOX");

        assertThat(userInfo.userKey()).isEqualTo("443731104");
        server.verify();
    }

    @Test
    void success로_감싸진_응답도_처리한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"resultType\":\"SUCCESS\",\"success\":{\"accessToken\":\"toss-access\"}}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(ME_URL))
                .andRespond(withSuccess("{\"resultType\":\"SUCCESS\",\"success\":{\"userKey\":7}}",
                        MediaType.APPLICATION_JSON));

        assertThat(tossClient.getUserInfo("code", "DEFAULT").userKey()).isEqualTo("7");
    }

    @Test
    void invalid_grant이면_인가_코드_오류로_처리한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"invalid_grant\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossClient.getUserInfo("used-code", "DEFAULT"))
                .isInstanceOf(TossAuthorizationCodeInvalidException.class);
    }

    @Test
    void resultType이_FAIL이면_토스_API_오류로_처리한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"resultType\":\"FAIL\",\"error\":{\"errorCode\":\"INTERNAL_ERROR\"}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossClient.getUserInfo("code", "DEFAULT")).isInstanceOf(TossApiException.class);
    }

    @Test
    void 토큰_발급이_서버_오류면_토스_API_오류로_처리한다() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> tossClient.getUserInfo("code", "DEFAULT")).isInstanceOf(TossApiException.class);
    }

    @Test
    void 사용자_조회에서_invalid_grant가_와도_인가_코드_오류로_보지_않는다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"accessToken\":\"toss-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ME_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"invalid_grant\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossClient.getUserInfo("code", "DEFAULT")).isInstanceOf(TossApiException.class);
    }

    @Test
    void userKey가_없으면_토스_API_오류로_처리한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"accessToken\":\"toss-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ME_URL))
                .andRespond(withSuccess("{\"scope\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossClient.getUserInfo("code", "DEFAULT")).isInstanceOf(TossApiException.class);
    }

    @Test
    void 실제_응답_모양으로_성공과_실패를_구분한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"resultType\":\"SUCCESS\",\"success\":{\"accessToken\":\"toss-access\"},"
                        + "\"error\":null}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ME_URL))
                .andRespond(withSuccess("{\"resultType\":\"SUCCESS\",\"success\":{\"userKey\":9},\"error\":null}",
                        MediaType.APPLICATION_JSON));

        assertThat(tossClient.getUserInfo("code", "DEFAULT").userKey()).isEqualTo("9");
    }

    @Test
    void 실제_실패_응답은_HTTP_200이어도_토스_API_오류로_처리한다() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"resultType\":\"FAIL\",\"success\":null,\"error\":{\"errorType\":0,"
                        + "\"errorCode\":\"4050\",\"reason\":\"등록된 미니앱이 아닙니다.\",\"data\":{},\"title\":null}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tossClient.getUserInfo("code", "DEFAULT")).isInstanceOf(TossApiException.class);
    }

    @Test
    void 응답_본문이_비어_있으면_토스_API_오류로_처리한다() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> tossClient.getUserInfo("code", "DEFAULT")).isInstanceOf(TossApiException.class);
    }
}
