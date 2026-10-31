package com.itta.toggleme.image.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.image.domain.ImageAnalysisResult;
import com.itta.toggleme.image.domain.ImageAnalysisStatus;
import com.itta.toggleme.image.dto.ImageAnalysisResponse;
import com.itta.toggleme.image.dto.ImageUploadUrlResponse;
import com.itta.toggleme.image.service.ImageAnalysisService;
import com.itta.toggleme.image.service.ImageService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ImageControllerTest {

    private static final long MEMBER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private ImageAnalysisService imageAnalysisService;

    @Test
    void 업로드_주소를_발급한다() throws Exception {
        given(imageService.createUploadUrl(MEMBER_ID, "image/jpeg", 1024L)).willReturn(new ImageUploadUrlResponse(
                "https://upload", "images/7/a.jpg", Map.of("Content-Type", "image/jpeg"), Instant.now()));

        request("image/jpeg", 1024)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value("https://upload"))
                .andExpect(jsonPath("$.imageKey").value("images/7/a.jpg"));
    }

    @Test
    void 지원하지_않는_형식이나_10MB를_넘는_이미지는_400을_반환한다() throws Exception {
        request("image/gif", 1024)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("jpeg, png, heic 이미지만 올릴 수 있습니다."));
        request("image/jpeg", 10 * 1024 * 1024 + 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("이미지는 10MB 이하만 올릴 수 있습니다."));
    }

    @Test
    void 인증_없이_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/images/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"contentLength\":1024}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 분석을_요청하면_202와_분석_ID를_반환한다() throws Exception {
        given(imageAnalysisService.requestAnalysis(MEMBER_ID, "images/7/a.jpg"))
                .willReturn(new ImageAnalysisResponse(1L, ImageAnalysisStatus.PENDING, null));

        mockMvc.perform(post("/api/v1/images/analysis")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageKey\":\"images/7/a.jpg\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 완료된_분석은_추천_태그를_반환한다() throws Exception {
        given(imageAnalysisService.getAnalysis(MEMBER_ID, 1L)).willReturn(new ImageAnalysisResponse(
                1L, ImageAnalysisStatus.COMPLETED, new ImageAnalysisResult(List.of("#추리", "#외국소설"))));

        mockMvc.perform(get("/api/v1/images/analysis/1")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.tags[0]").value("#추리"))
                .andExpect(jsonPath("$.result.tags[1]").value("#외국소설"));
    }

    private ResultActions request(String contentType, long contentLength) throws Exception {
        return mockMvc.perform(post("/api/v1/images/upload-url")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentType\":\"%s\",\"contentLength\":%d}".formatted(contentType, contentLength)));
    }
}
