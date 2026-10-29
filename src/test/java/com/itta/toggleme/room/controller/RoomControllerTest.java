package com.itta.toggleme.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomFloorRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomControllerTest {

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
    private RoomService roomService;

    private Member owner;
    private Long roomId;
    private Long floorId;
    private List<FurnitureType> furnitureTypes;
    private List<FurnitureVariant> furnitureVariants;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        saveCatalog();
        owner = saveMember();
        roomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF);
        floorId = roomFloorRepository.findAllByRoomIdOrderByFloorNumber(roomId).getFirst().getId();
    }

    @Test
    void 내_방_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/rooms").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(roomId))
                .andExpect(jsonPath("$[0].visibility").value("SOCIAL_OFF"));
    }

    @Test
    void 내_방을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}", roomId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("나의 방"));
    }

    @Test
    void 다른_회원의_방은_조회할_수_없다() throws Exception {
        Member other = saveMember();

        mockMvc.perform(get("/api/v1/rooms/{roomId}", roomId).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void 방의_층_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}/floors", roomId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].floorNumber").value(1));
    }

    @Test
    void 방_공개_설정을_변경한다() throws Exception {
        mockMvc.perform(patch("/api/v1/rooms/{roomId}/visibility", roomId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"SOCIAL_ON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("SOCIAL_ON"));
    }

    @Test
    void 다른_회원의_방_공개_설정은_변경할_수_없다() throws Exception {
        Member other = saveMember();

        mockMvc.perform(patch("/api/v1/rooms/{roomId}/visibility", roomId)
                        .header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"SOCIAL_ON\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void 공개_설정_값이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/rooms/{roomId}/visibility", roomId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 공개_설정_숫자_값은_400을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/v1/rooms/{roomId}/visibility", roomId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 방_ID가_숫자가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}", "abc").header("Authorization", bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 층_가구에_숨김_합성_항목을_포함한다() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].furnitureTypeId").value(furnitureTypes.get(0).getId()))
                .andExpect(jsonPath("$[0].variantId").value(furnitureVariants.get(0).getId()))
                .andExpect(jsonPath("$[0].hidden").value(true))
                .andExpect(jsonPath("$[0].roomFurnitureId").doesNotExist());
    }

    @Test
    void 층_가구_전체를_저장하고_조회와_같은_형식으로_응답한다() throws Exception {
        mockMvc.perform(put("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFurnitureJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].furnitureTypeCode").value(furnitureTypes.get(0).getCode()))
                .andExpect(jsonPath("$[0].positionX").value(1.0))
                .andExpect(jsonPath("$[0].renderOrder").value(1))
                .andExpect(jsonPath("$[4].hidden").value(true));
    }

    @Test
    void 다른_회원의_층_가구는_조회할_수_없다() throws Exception {
        Member other = saveMember();

        mockMvc.perform(get("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .header("Authorization", bearer(other)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void 다른_방의_층_ID로_저장할_수_없다() throws Exception {
        Member other = saveMember();
        Long otherRoomId = roomService.createDefaultRoom(other, List.of(), RoomVisibility.SOCIAL_OFF);
        Long otherFloorId = roomFloorRepository.findAllByRoomIdOrderByFloorNumber(otherRoomId).getFirst().getId();

        mockMvc.perform(put("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, otherFloorId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFurnitureJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void 미인증_층_가구_조회와_저장은_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFurnitureJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 가구_배열의_null_요소는_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furniture\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 가구_필드의_null과_잘못된_JSON_타입은_400을_반환한다() throws Exception {
        mockMvc.perform(put("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furniture\":[{\"furnitureTypeId\":null,\"variantId\":null,\"hidden\":null}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        mockMvc.perform(put("/api/v1/rooms/{roomId}/floors/{floorId}/furniture", roomId, floorId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"furniture\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    private Member saveMember() {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(Member.create(handle, "토글러"));
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    private void saveCatalog() {
        furnitureTypes = new ArrayList<>();
        furnitureVariants = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            FurnitureType type = FurnitureType.create(uniqueCode(), "가구 " + i, i);
            type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
            furnitureTypes.add(furnitureTypeRepository.save(type));
            furnitureVariants.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "기본 " + i, uniqueCode(), 1, 1)));
        }
    }

    private String validFurnitureJson() {
        StringBuilder json = new StringBuilder("{\"furniture\":[");
        for (int i = 0; i < furnitureTypes.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"furnitureTypeId\":").append(furnitureTypes.get(i).getId())
                    .append(",\"variantId\":").append(furnitureVariants.get(i).getId())
                    .append(",\"hidden\":").append(i == 4).append('}');
        }
        return json.append("]}").toString();
    }

    private String uniqueCode() {
        return UUID.randomUUID().toString();
    }
}
