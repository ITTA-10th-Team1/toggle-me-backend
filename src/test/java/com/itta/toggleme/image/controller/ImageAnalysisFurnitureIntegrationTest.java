package com.itta.toggleme.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.image.service.ImageAnalysisWorker;
import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImageAnalysisFurnitureIntegrationTest {

    private static final String IMAGE_KEY = "images/1/a.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @MockitoBean
    private ImageService imageService;

    // 실제 Gemini를 부르지 않는다
    @MockitoBean
    private ImageAnalysisWorker imageAnalysisWorker;

    private Long memberId;
    private Long fridgeId;
    private Long mirrorId;

    @BeforeEach
    void setUp() {
        given(imageService.isUploadedBy(anyLong(), anyString())).willReturn(true);
        memberId = memberRepository.save(
                Member.create("user_" + UUID.randomUUID().toString().substring(0, 8), "토글러")).getId();
        fridgeId = furnitureTypeRepository.save(FurnitureType.create("fridge_" + suffix(), "냉장고", 1)).getId();
        mirrorId = furnitureTypeRepository.save(FurnitureType.create("mirror_" + suffix(), "거울", 2)).getId();
    }

    @Test
    void 가구를_보내면_분석을_요청할_수_있다() throws Exception {
        requestAnalysis(fridgeId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 가구_없이도_요청할_수_있다() throws Exception {
        requestAnalysis(null).andExpect(status().isAccepted());
    }

    @Test
    void 없는_가구면_404를_반환한다() throws Exception {
        requestAnalysis(999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FURNITURE_TYPE_NOT_FOUND"));
    }

    @Test
    void 같은_사진을_같은_가구로_다시_요청하면_기존_분석을_돌려준다() throws Exception {
        Number first = analysisId(requestAnalysis(fridgeId));
        Number second = analysisId(requestAnalysis(fridgeId));

        assertThat(second.longValue()).isEqualTo(first.longValue());
    }

    @Test
    void 같은_사진이라도_가구가_다르면_새로_분석한다() throws Exception {
        Number fridge = analysisId(requestAnalysis(fridgeId));

        requestAnalysis(mirrorId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.analysisId").value(not(fridge.intValue())));
    }

    private Number analysisId(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.analysisId");
    }

    private ResultActions requestAnalysis(Long furnitureTypeId) throws Exception {
        String furniture = furnitureTypeId == null ? "" : ",\"furnitureTypeId\":" + furnitureTypeId;
        return mockMvc.perform(post("/api/v1/images/analysis")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"imageKey\":\"" + IMAGE_KEY + "\"" + furniture + "}"));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}
