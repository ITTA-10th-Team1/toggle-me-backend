package com.itta.toggleme.follow.controller;

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
import com.itta.toggleme.room.repository.RoomRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import com.itta.toggleme.room.service.RoomService;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SharedRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomRepository roomRepository;

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

    private List<Long> placedTypeIds;
    private List<Long> placedVariantIds;
    private Long notPlacedTypeId;
    private Member owner;
    private Member viewer;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        saveCatalog();
        given(imageService.isUploadedBy(anyLong(), anyString())).willReturn(true);
        given(imageService.createViewUrl(anyString())).willAnswer(call -> "https://view/" + call.getArgument(0));
        owner = saveMemberWithRoom(RoomVisibility.SOCIAL_ON);
        viewer = saveMemberWithRoom(RoomVisibility.SOCIAL_ON);
    }

    @Test
    void 남의_방을_열면_켜진_가구와_방_태그를_준다() throws Exception {
        record(placedTypeIds.get(0), "PUBLIC", "빵");

        mockMvc.perform(get(roomPath()).header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.furniture.length()").value(4))
                .andExpect(jsonPath("$.furniture[?(@.furnitureTypeId == " + notPlacedTypeId + ")]").isEmpty())
                .andExpect(jsonPath("$.furniture[0].position").exists())
                .andExpect(jsonPath("$.wallStyleId").exists())
                .andExpect(jsonPath("$.tags[0].furnitureTypeId").value(placedTypeIds.get(0)))
                .andExpect(jsonPath("$.tags[0].name").value("빵"));
    }

    @Test
    void 남에게는_공개_기록만_최근_순으로_주고_주인에게는_비공개도_준다() throws Exception {
        Long typeId = placedTypeIds.get(0);
        long first = record(typeId, "PUBLIC", "빵");
        long hidden = record(typeId, "PRIVATE", "비밀");
        long last = record(typeId, "PUBLIC", "커피");

        mockMvc.perform(get(itemsPath(typeId)).header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(last))
                .andExpect(jsonPath("$[0].tags[0]").value("커피"))
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[1].id").value(first));

        mockMvc.perform(get(itemsPath(typeId)).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[1].id").value(hidden))
                .andExpect(jsonPath("$[1].visibility").value("PRIVATE"));
    }

    @Test
    void 기록은_20개씩_주고_before로_다음_장을_받는다() throws Exception {
        Long typeId = placedTypeIds.get(1);
        long oldest = record(typeId, "PUBLIC", "a");
        for (int i = 0; i < 20; i++) {
            record(typeId, "PUBLIC", "a");
        }

        String page = mockMvc.perform(get(itemsPath(typeId)).header("Authorization", bearer(viewer)))
                .andExpect(jsonPath("$.length()").value(20))
                .andReturn().getResponse().getContentAsString();
        long lastId = ((Number) JsonPath.read(page, "$[19].id")).longValue();

        mockMvc.perform(get(itemsPath(typeId)).param("before", String.valueOf(lastId)).header("Authorization", bearer(viewer)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(oldest));
    }

    @Test
    void 공개_기록이_없으면_빈_목록이다() throws Exception {
        record(placedTypeIds.get(0), "PRIVATE", "비밀");

        mockMvc.perform(get(itemsPath(placedTypeIds.get(0))).header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 방에_두지_않은_가구의_기록은_404다() throws Exception {
        mockMvc.perform(get(itemsPath(notPlacedTypeId)).header("Authorization", bearer(viewer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FURNITURE_TYPE_NOT_FOUND"));
    }

    @Test
    void 방이_OFF면_남은_못_열고_주인은_연다() throws Exception {
        roomRepository.findFirstByOwnerIdOrderById(owner.getId()).orElseThrow()
                .changeVisibility(RoomVisibility.SOCIAL_OFF);

        mockMvc.perform(get(roomPath()).header("Authorization", bearer(viewer)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(itemsPath(placedTypeIds.get(0))).header("Authorization", bearer(viewer)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(roomPath()).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
    }

    private long record(Long furnitureTypeId, String visibility, String tag) throws Exception {
        String key = "images/" + owner.getId() + "/" + UUID.randomUUID() + ".jpg";
        String body = mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageKey":"%s","furnitureTypeId":%d,"visibility":"%s","tags":["%s"]}
                                """.formatted(key, furnitureTypeId, visibility, tag)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String roomPath() {
        return "/api/v1/members/" + owner.getHandle() + "/room";
    }

    private String itemsPath(Long furnitureTypeId) {
        return roomPath() + "/items?furnitureTypeId=" + furnitureTypeId;
    }

    private Member saveMemberWithRoom(RoomVisibility visibility) {
        Member member = memberRepository.save(Member.create("user_" + UUID.randomUUID().toString().substring(0, 8), "토글러"));
        member.updateProfile("room_" + UUID.randomUUID().toString().substring(0, 8), "토글러");
        roomService.createDefaultRoom(member, placedVariantIds, visibility);
        return member;
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    // 방에는 가구 4개를 둔다. 다섯째 가구는 카탈로그에만 있고 방에 두지 않는다
    private void saveCatalog() {
        placedTypeIds = new ArrayList<>();
        placedVariantIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            FurnitureType type = FurnitureType.create(UUID.randomUUID().toString(), "가구 " + i, i);
            type.configurePlacement(Placement.at(i, 0, 0), i);
            furnitureTypeRepository.save(type);
            Long variantId = furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "기본 " + i, UUID.randomUUID().toString(), 1, 1)).getId();
            if (i == 5) {
                notPlacedTypeId = type.getId();
            } else {
                placedTypeIds.add(type.getId());
                placedVariantIds.add(variantId);
            }
        }
    }
}
