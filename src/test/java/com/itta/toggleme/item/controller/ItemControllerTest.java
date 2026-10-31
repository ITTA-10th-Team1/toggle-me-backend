package com.itta.toggleme.item.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.domain.FurnitureVariant;
import com.itta.toggleme.room.domain.RoomStyle;
import com.itta.toggleme.room.domain.RoomStyleType;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.FurnitureVariantRepository;
import com.itta.toggleme.room.repository.RoomStyleRepository;
import com.itta.toggleme.room.service.RoomService;
import java.math.BigDecimal;
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
class ItemControllerTest {

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

    private Long fridgeTypeId;
    private Long fridgeVariantId;
    private List<Long> placedTypeIds;
    private List<Long> placedVariantIds;
    private Long notPlacedTypeId;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        saveCatalog();
        given(imageService.isUploadedBy(anyLong(), anyString())).willReturn(true);
        given(imageService.createViewUrl(anyString())).willAnswer(call -> "https://view/" + call.getArgument(0));
    }

    @Test
    void 방에_둔_가구에_아이템을_기록한다() throws Exception {
        Member member = saveMemberWithRoom(false);
        String key = imageKey(member);

        create(member, key, placedTypeIds.get(1), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.furnitureTypeId").value(placedTypeIds.get(1)))
                .andExpect(jsonPath("$.recordedOn").value(TODAY.toString()))
                .andExpect(jsonPath("$.imageUrl").value("https://view/" + key))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    void 방에_없는_가구에는_기록할_수_없다() throws Exception {
        Member member = saveMemberWithRoom(false);

        create(member, imageKey(member), notPlacedTypeId, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ITEM_FURNITURE_NOT_SELECTED"));
    }

    @Test
    void 없는_가구_종류는_404를_반환한다() throws Exception {
        Member member = saveMemberWithRoom(false);

        create(member, imageKey(member), Long.MAX_VALUE, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FURNITURE_TYPE_NOT_FOUND"));
    }

    @Test
    void 냉장고는_통합_API에서도_냉장고_규칙을_따른다() throws Exception {
        Member withFridge = saveMemberWithRoom(true);
        create(withFridge, imageKey(withFridge), fridgeTypeId, TODAY.plusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_RECORDED_DATE_INVALID"));
        create(withFridge, imageKey(withFridge), fridgeTypeId, TODAY.minusDays(2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordedOn").value(TODAY.minusDays(2).toString()));

        Member withoutFridge = saveMemberWithRoom(false);
        create(withoutFridge, imageKey(withoutFridge), fridgeTypeId, TODAY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FRIDGE_NOT_SELECTED"));
    }

    @Test
    void 가구_종류와_공개_여부가_없으면_400을_반환한다() throws Exception {
        Member member = saveMemberWithRoom(false);

        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageKey\":\"%s\"}".formatted(imageKey(member))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    private ResultActions create(Member member, String key, Long furnitureTypeId, LocalDate recordedOn)
            throws Exception {
        String recordedOnJson = recordedOn == null ? "null" : "\"" + recordedOn + "\"";
        return mockMvc.perform(post("/api/v1/items")
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageKey":"%s","furnitureTypeId":%d,"recordedOn":%s,"visibility":"PUBLIC"}
                        """.formatted(key, furnitureTypeId, recordedOnJson)));
    }

    private String imageKey(Member member) {
        return "images/" + member.getId() + "/" + UUID.randomUUID() + ".jpg";
    }

    private Member saveMemberWithRoom(boolean withFridge) {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Member member = memberRepository.save(Member.create(handle, "토글러"));
        List<Long> variantIds = new ArrayList<>(placedVariantIds);
        if (withFridge) {
            variantIds.set(0, fridgeVariantId);
        }
        roomService.createDefaultRoom(member, variantIds, RoomVisibility.SOCIAL_ON);
        return member;
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    // 방에는 가구 4개를 둔다. 냉장고가 있는 방은 첫 가구를 냉장고로 바꾼다. 다섯째 가구는 방에 두지 않는다
    private void saveCatalog() {
        FurnitureType fridge = furnitureTypeRepository.findByCode("FRIDGE")
                .orElseGet(() -> furnitureTypeRepository.save(FurnitureType.create("FRIDGE", "냉장고", 99)));
        fridge.configurePlacement(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, 99);
        fridgeTypeId = fridge.getId();
        fridgeVariantId = furnitureVariantRepository.save(
                FurnitureVariant.create(fridge, "기본 냉장고", UUID.randomUUID().toString(), 1, 1)).getId();

        placedTypeIds = new ArrayList<>();
        placedVariantIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            FurnitureType type = FurnitureType.create(UUID.randomUUID().toString(), "가구 " + i, i);
            type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
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
