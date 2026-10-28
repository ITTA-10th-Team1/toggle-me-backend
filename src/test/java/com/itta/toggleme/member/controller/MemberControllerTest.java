package com.itta.toggleme.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomFurnitureRepository;
import com.itta.toggleme.room.repository.RoomRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomFurnitureRepository roomFurnitureRepository;

    @Autowired
    private RoomStyleRepository roomStyleRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private FurnitureVariantRepository furnitureVariantRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        member = memberRepository.save(Member.create("user_" + randomSuffix(), "토글러"));
        member.updateProfile("member_" + randomSuffix(), "토글러");
    }

    @Test
    void 핸들_사용_가능_여부를_검사하고_대소문자와_공백을_정규화한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/handle-availability")
                        .param("handle", "  " + member.getHandle().toUpperCase() + "  ")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 잘못된_핸들과_예약_핸들은_사용할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/members/handle-availability").param("handle", "ab")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("INVALID_FORMAT"));
        mockMvc.perform(get("/api/v1/members/handle-availability").param("handle", "user_temp123")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("RESERVED"));
        mockMvc.perform(get("/api/v1/members/handle-availability").param("handle", "admin")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("RESERVED"));
    }

    @Test
    void 다른_회원_핸들은_중복이고_임시_본인_핸들은_예약어다() throws Exception {
        Member other = memberRepository.save(Member.create("other_" + randomSuffix(), "다른 회원"));
        mockMvc.perform(get("/api/v1/members/handle-availability").param("handle", other.getHandle())
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("DUPLICATED"));
        Member temporary = memberRepository.save(Member.create("user_" + randomSuffix(), "임시"));
        mockMvc.perform(get("/api/v1/members/handle-availability").param("handle", temporary.getHandle())
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(temporary.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reason").value("RESERVED"));
    }

    @Test
    void 미인증_핸들_조회는_401이다() throws Exception {
        mockMvc.perform(get("/api/v1/members/handle-availability").param("handle", "valid_name"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 프로필을_저장하고_같은_값으로_다시_저장할_수_있다() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"  New_Name  \",\"nickname\":\" 지인 \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.handle").value("new_name"))
                .andExpect(jsonPath("$.nickname").value("지인"));
        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"new_name\",\"nickname\":\"지인\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 다른_회원이_선점한_핸들은_프로필_저장시_409다() throws Exception {
        Member other = memberRepository.save(Member.create("taken_handle", "다른 회원"));
        mockMvc.perform(patch("/api/v1/members/me/profile").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"" + other.getHandle() + "\",\"nickname\":\"이름\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HANDLE_ALREADY_EXISTS"));
    }

    @Test
    void 온보딩_완료_후에는_프로필을_수정할_수_없다() throws Exception {
        member.completeOnboarding();
        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"new_name\",\"nickname\":\"다른 이름\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_ONBOARDING_ALREADY_COMPLETED"));
    }

    @Test
    void 프로필을_저장하지_않으면_가구_온보딩은_409다() throws Exception {
        Member incomplete = memberRepository.save(Member.create("user_" + randomSuffix(), "토글러"));
        mockMvc.perform(post("/api/v1/members/onboarding")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(incomplete.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(List.of(1L, 2L, 3L, 4L))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER_PROFILE_REQUIRED"));
    }

    @Test
    void 프로필_요청_검증_실패는_400이다() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/profile").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"ab\",\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 프로필_핸들_형식과_예약어_위반은_각각_400이다() throws Exception {
        mockMvc.perform(patch("/api/v1/members/me/profile").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"ab\",\"nickname\":\"이름\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("HANDLE_INVALID"));
        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"user_custom\",\"nickname\":\"이름\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("HANDLE_RESERVED"));
    }

    @Test
    void 온보딩을_완료하면_방과_선택한_가구만_생성한다() throws Exception {
        List<FurnitureVariant> variants = saveFurnitureVariants(5);
        List<Long> selectedIds = variants.subList(0, 4).stream().map(FurnitureVariant::getId).toList();

        mockMvc.perform(post("/api/v1/members/onboarding")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(selectedIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").isNumber());

        assertThat(member.isOnboardingCompleted()).isTrue();
        assertThat(roomRepository.findAllByOwnerIdOrderById(member.getId())).hasSize(1);
        assertThat(roomFurnitureRepository.findAll())
                .hasSize(4)
                .allSatisfy(furniture -> assertThat(furniture.getPositionX()).isNotNull());
    }

    @Test
    void 가구를_3개만_선택하면_온보딩할_수_없다() throws Exception {
        List<Long> selectedIds = saveFurnitureVariants(3).stream().map(FurnitureVariant::getId).toList();

        mockMvc.perform(post("/api/v1/members/onboarding")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(selectedIds)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 같은_종류의_디자인을_두_개_선택하면_온보딩할_수_없다() throws Exception {
        List<FurnitureVariant> variants = saveFurnitureVariants(3);
        FurnitureType firstType = variants.getFirst().getFurnitureType();
        FurnitureVariant duplicateTypeVariant = furnitureVariantRepository.save(
                FurnitureVariant.create(firstType, "다른 디자인", "furniture/duplicate.glb", 1, 1)
        );
        List<Long> selectedIds = List.of(
                variants.get(0).getId(),
                duplicateTypeVariant.getId(),
                variants.get(1).getId(),
                variants.get(2).getId()
        );

        mockMvc.perform(post("/api/v1/members/onboarding")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(selectedIds)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FURNITURE_SELECTION"));

        assertThat(roomRepository.findAllByOwnerIdOrderById(member.getId())).isEmpty();
        assertThat(member.isOnboardingCompleted()).isFalse();
    }

    @Test
    void 고정_위치가_없는_가구는_배치하지_않는다() throws Exception {
        List<FurnitureVariant> variants = saveFurnitureVariants(3);
        FurnitureType type = furnitureTypeRepository.save(
                FurnitureType.create("UNPLACED_" + randomSuffix(), "좌표 미정 가구", 4)
        );
        FurnitureVariant unplaced = furnitureVariantRepository.save(
                FurnitureVariant.create(type, "기본 디자인", "furniture/unplaced.glb", 1, 1)
        );
        List<Long> selectedIds = List.of(
                variants.get(0).getId(),
                variants.get(1).getId(),
                variants.get(2).getId(),
                unplaced.getId()
        );

        mockMvc.perform(post("/api/v1/members/onboarding")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(selectedIds)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FURNITURE_PLACEMENT_NOT_CONFIGURED"));

        assertThat(roomRepository.findAllByOwnerIdOrderById(member.getId())).isEmpty();
        assertThat(member.isOnboardingCompleted()).isFalse();
    }

    private List<FurnitureVariant> saveFurnitureVariants(int count) {
        List<FurnitureVariant> variants = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            FurnitureType type = FurnitureType.create("TYPE_" + i + randomSuffix(), "가구 " + i, i);
            type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
            furnitureTypeRepository.save(type);
            variants.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "기본 디자인", "furniture/type-" + i + ".glb", 1, 1)
            ));
        }
        return variants;
    }

    private String requestBody(List<Long> ids) {
        String values = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return "{\"furnitureVariantIds\":[" + values + "]}";
    }

    private String bearer() {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
