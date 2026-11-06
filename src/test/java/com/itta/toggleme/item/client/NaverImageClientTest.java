package com.itta.toggleme.item.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NaverImageClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final NaverImageClient client = new NaverImageClient(new NaverProperties("id", "secret"), builder);

    @Test
    void 첫_번째_https_썸네일_주소를_돌려주고_키를_헤더로_보낸다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://naverapihub.apigw.ntruss.com/search/v1/image?query=")))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", "id"))
                .andExpect(header("X-NCP-APIGW-API-KEY", "secret"))
                .andRespond(withSuccess("""
                        {"items":[{"thumbnail":"http://old/a.jpg"},{"thumbnail":"https://search.pstatic.net/b.jpg","link":"x"}]}
                        """, MediaType.TEXT_PLAIN));

        assertThat(client.findImageUrl("까르보나라")).contains("https://search.pstatic.net/b.jpg");
    }

    @Test
    void 결과가_없으면_빈_값이다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://naverapihub.apigw.ntruss.com/")))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

        assertThat(client.findImageUrl("없는음식")).isEmpty();
    }

    @Test
    void 호출_자체가_실패하면_결과_없음과_구분해_예외를_던진다() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://naverapihub.apigw.ntruss.com/")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findImageUrl("파스타")).isInstanceOf(NaverSearchException.class);
    }
}
