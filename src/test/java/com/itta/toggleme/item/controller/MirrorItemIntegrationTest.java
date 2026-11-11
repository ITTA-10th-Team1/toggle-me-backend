package com.itta.toggleme.item.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.Placement;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import com.itta.toggleme.room.service.RoomService;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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
class MirrorItemIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomStyleRepository roomStyleRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private FurnitureVariantRepository furnitureVariantRepository;

    @Autowired
    private RoomService roomService;

    @MockitoBean
    private ImageService imageService;

    private Long mirrorTypeId;
    private Long mirrorVariantId;
    private List<Long> otherVariantIds;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        FurnitureType mirror = furnitureTypeRepository.findByCode("mirror")
                .orElseGet(() -> furnitureTypeRepository.save(FurnitureType.create("mirror", "거울", 98)));
        mirror.configurePlacement(Placement.at(20, 0, 0), 98);
        mirrorTypeId = mirror.getId();
        mirrorVariantId = saveVariant(mirror);

        // 방에는 가구가 최소 4개 있어야 한다. 거울이 없는 방은 다른 가구 4개를 둔다
        otherVariantIds = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            FurnitureType other = FurnitureType.create(UUID.randomUUID().toString(), "다른 가구 " + i, i);
            other.configurePlacement(Placement.at(i, 0, 0), i);
            furnitureTypeRepository.save(other);
            otherVariantIds.add(saveVariant(other));
        }

        given(imageService.isUploadedBy(anyLong(), anyString())).willReturn(true);
        given(imageService.createViewUrl(anyString())).willAnswer(call -> "https://view/" + call.getArgument(0));
    }

    @Test
    void 거울에_착장_사진을_기록하면_오늘로_자동_기록한다() throws Exception {
        Member member = saveMemberWithRoom(true);

        create(member, TODAY.minusDays(30))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.furnitureTypeId").value(mirrorTypeId))
                .andExpect(jsonPath("$.recordedOn").value(TODAY.toString()));
    }

    @Test
    void 거울은_하루_10장까지_기록할_수_있다() throws Exception {
        Member member = saveMemberWithRoom(true);
        for (int i = 0; i < 10; i++) {
            create(member, null).andExpect(status().isCreated());
        }

        create(member, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ITEM_DAILY_LIMIT_EXCEEDED"));
    }

    @Test
    void 방에_거울이_없으면_403을_반환한다() throws Exception {
        Member member = saveMemberWithRoom(false);

        create(member, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ITEM_FURNITURE_NOT_SELECTED"));
    }

    @Test
    void 기록한_착장을_날짜별로_조회한다() throws Exception {
        Member member = saveMemberWithRoom(true);
        create(member, null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/items")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
                        .param("furnitureTypeId", mirrorTypeId.toString())
                        .param("date", TODAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 가구별_피드는_최신순이고_cursor로_다음_쪽을_준다() throws Exception {
        Member member = saveMemberWithRoom(true);
        for (int i = 0; i < 3; i++) {
            create(member, null).andExpect(status().isCreated());
        }

        String first = feed(member, mirrorTypeId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(first, "$[*].id");
        assertThat(ids.get(0).longValue()).isGreaterThan(ids.get(1).longValue());

        feed(member, mirrorTypeId, ids.get(1).longValue())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ids.get(2).longValue()));
    }

    @Test
    void 피드에는_다른_회원의_기록이_나오지_않는다() throws Exception {
        Member owner = saveMemberWithRoom(true);
        Member other = saveMemberWithRoom(true);
        create(owner, null).andExpect(status().isCreated());

        feed(other, mirrorTypeId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 피드는_sort가_OLDEST면_오래된_순으로_준다() throws Exception {
        Member member = saveMemberWithRoom(true);
        for (int i = 0; i < 3; i++) {
            create(member, null).andExpect(status().isCreated());
        }

        String body = mockMvc.perform(get("/api/v1/items/feed")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
                        .param("furnitureTypeId", mirrorTypeId.toString())
                        .param("sort", "OLDEST"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[*].id");
        assertThat(ids).hasSize(3);
        assertThat(ids.get(0).longValue()).isLessThan(ids.get(1).longValue());
    }

    @Test
    void 방에서_꺼진_가구의_피드는_404를_반환한다() throws Exception {
        Member member = saveMemberWithRoom(false);

        feed(member, mirrorTypeId, null)
                .andExpect(status().isNotFound());
    }

    private ResultActions feed(Member member, Long furnitureTypeId, Long cursor) throws Exception {
        var request = get("/api/v1/items/feed")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
                .param("furnitureTypeId", furnitureTypeId.toString());
        if (cursor != null) {
            request.param("cursor", cursor.toString());
        }
        return mockMvc.perform(request);
    }

    private ResultActions create(Member member, LocalDate recordedOn) throws Exception {
        String key = "images/" + member.getId() + "/" + UUID.randomUUID() + ".jpg";
        String recordedOnJson = recordedOn == null ? "null" : "\"" + recordedOn + "\"";
        return mockMvc.perform(post("/api/v1/items")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(member.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageKey":"%s","furnitureTypeId":%d,"recordedOn":%s,"visibility":"PUBLIC"}
                        """.formatted(key, mirrorTypeId, recordedOnJson)));
    }

    private Member saveMemberWithRoom(boolean withMirror) {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Member member = memberRepository.save(Member.create(handle, "토글러"));
        List<Long> variantIds = new ArrayList<>(otherVariantIds);
        if (withMirror) {
            variantIds.set(0, mirrorVariantId);
        }
        roomService.createDefaultRoom(member, variantIds, RoomVisibility.SOCIAL_ON);
        return member;
    }

    private Long saveVariant(FurnitureType type) {
        return furnitureVariantRepository.save(
                FurnitureVariant.create(type, "기본", UUID.randomUUID().toString(), 1, 1)).getId();
    }
}
