package com.itta.toggleme.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import com.itta.toggleme.room.service.RoomService;
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
    private RoomService roomService;

    private Member owner;
    private Long roomId;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        owner = saveMember();
        roomId = roomService.createDefaultRoom(owner, List.of(), RoomVisibility.SOCIAL_OFF);
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

    private Member saveMember() {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        return memberRepository.save(Member.create(handle, "토글러"));
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }
}
