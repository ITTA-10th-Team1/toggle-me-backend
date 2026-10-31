package com.itta.toggleme.room.controller;

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
import com.itta.toggleme.room.domain.RoomFurniture;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomFloorRepository;
import com.itta.toggleme.room.repository.RoomFurnitureRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import com.itta.toggleme.room.service.RoomService;
import java.math.BigDecimal;
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
class RoomTagIntegrationTest {

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
    private RoomFloorRepository roomFloorRepository;

    @Autowired
    private RoomFurnitureRepository roomFurnitureRepository;

    @Autowired
    private RoomService roomService;

    @MockitoBean
    private ImageService imageService;

    private List<Long> typeIds;
    private List<Long> variantIds;
    private Member owner;
    private Long roomId;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        saveCatalog();
        given(imageService.isUploadedBy(anyLong(), anyString())).willReturn(true);
        given(imageService.createViewUrl(anyString())).willReturn("https://view");
        owner = memberRepository.save(Member.create("user_" + UUID.randomUUID().toString().substring(0, 8), "토글러"));
        roomId = roomService.createDefaultRoom(owner, variantIds, RoomVisibility.SOCIAL_ON);
    }

    @Test
    void 공개_아이템의_태그로_가구별_대표_태그를_보여준다() throws Exception {
        record(typeIds.get(0), "PUBLIC", "[\"추리\", \"외국소설\"]");
        record(typeIds.get(1), "PUBLIC", "[\"추리\", \"스릴러\"]");

        tags(owner, roomId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].furnitureTypeId").value(typeIds.get(1)))
                .andExpect(jsonPath("$[0].name").value("추리"))
                .andExpect(jsonPath("$[1].furnitureTypeId").value(typeIds.get(0)))
                .andExpect(jsonPath("$[1].name").value("외국소설"));
    }

    @Test
    void 비공개_아이템의_태그는_넣지_않는다() throws Exception {
        record(typeIds.get(0), "PRIVATE", "[\"다이어트\"]");

        tags(owner, roomId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 숨긴_가구의_태그는_넣지_않고_다시_켜면_돌아온다() throws Exception {
        record(typeIds.get(0), "PUBLIC", "[\"추리\"]");
        RoomFurniture desk = roomFurniture(typeIds.get(0));

        desk.customize(desk.getVariant(), true);
        tags(owner, roomId).andExpect(jsonPath("$.length()").value(0));

        desk.customize(desk.getVariant(), false);
        tags(owner, roomId).andExpect(jsonPath("$[0].name").value("추리"));
    }

    @Test
    void 다른_회원의_방은_조회할_수_없다() throws Exception {
        Member other = memberRepository.save(
                Member.create("user_" + UUID.randomUUID().toString().substring(0, 8), "다른 토글러"));

        tags(other, roomId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    private void record(Long furnitureTypeId, String visibility, String tagsJson) throws Exception {
        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageKey":"images/%d/%s.jpg","furnitureTypeId":%d,"visibility":"%s","tags":%s}
                                """.formatted(owner.getId(), UUID.randomUUID(), furnitureTypeId, visibility, tagsJson)))
                .andExpect(status().isCreated());
    }

    private ResultActions tags(Member member, Long roomId) throws Exception {
        return mockMvc.perform(get("/api/v1/rooms/{roomId}/tags", roomId).header("Authorization", bearer(member)));
    }

    private RoomFurniture roomFurniture(Long furnitureTypeId) {
        Long floorId = roomFloorRepository.findAllByRoomIdOrderByFloorNumber(roomId).getFirst().getId();
        return roomFurnitureRepository.findAllByFloorId(floorId).stream()
                .filter(furniture -> furniture.getVariant().getFurnitureType().getId().equals(furnitureTypeId))
                .findFirst()
                .orElseThrow();
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    private void saveCatalog() {
        typeIds = new ArrayList<>();
        variantIds = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            FurnitureType type = FurnitureType.create(UUID.randomUUID().toString(), "가구 " + i, i);
            type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
            furnitureTypeRepository.save(type);
            typeIds.add(type.getId());
            variantIds.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "기본 " + i, UUID.randomUUID().toString(), 1, 1)).getId());
        }
    }
}
