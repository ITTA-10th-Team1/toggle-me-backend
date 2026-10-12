package com.itta.toggleme.room.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import java.util.UUID;
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
class FurnitureTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private FurnitureVariantRepository furnitureVariantRepository;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = "Bearer " + jwtTokenProvider.createAccessToken(1L);
    }

    @Test
    void 가구_종류의_디자인_목록을_조회한다() throws Exception {
        FurnitureType type = furnitureTypeRepository.save(FurnitureType.create(uniqueCode(), "전신 거울", 1));
        furnitureVariantRepository.save(FurnitureVariant.create(type, "둥근 거울", "furniture/mirror/round.glb", 1, 2));

        mockMvc.perform(get("/api/v1/furniture-types/{furnitureTypeId}/variants", type.getId())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("둥근 거울"))
                .andExpect(jsonPath("$[0].furnitureTypeId").value(type.getId()));
    }

    @Test
    void 없는_가구_종류의_디자인은_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/furniture-types/{furnitureTypeId}/variants", Long.MAX_VALUE)
                        .header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FURNITURE_TYPE_NOT_FOUND"));
    }

    @Test
    void 가구_종류_목록을_조회한다() throws Exception {
        furnitureTypeRepository.save(FurnitureType.create(uniqueCode(), "전신 거울", 1));

        mockMvc.perform(get("/api/v1/furniture-types").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '전신 거울')]").exists());
    }

    private String uniqueCode() {
        return "TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
