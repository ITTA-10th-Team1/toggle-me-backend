package com.itta.toggleme.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomStyleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RoomStyleRepository roomStyleRepository;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = "Bearer " + jwtTokenProvider.createAccessToken(1L);
    }

    @Test
    void 유형별_방_스타일_목록을_조회한다() throws Exception {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "벽돌 벽", "room/wall/brick.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "나무 바닥", "room/floor/wood.glb"));

        mockMvc.perform(get("/api/v1/room-styles").param("type", "WALL").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '벽돌 벽')]").exists())
                .andExpect(jsonPath("$[?(@.name == '나무 바닥')]").doesNotExist());
    }

    @Test
    void 스타일_유형이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/room-styles").header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 잘못된_스타일_유형이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/room-styles").param("type", "CEILING").header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
