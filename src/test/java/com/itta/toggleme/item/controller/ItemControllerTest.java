package com.itta.toggleme.item.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.item.repository.ItemTagRepository;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private ItemTagRepository itemTagRepository;

    @Autowired
    private EntityManager entityManager;

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
    void 고른_태그는_샵과_공백을_떼고_중복을_빼서_순서대로_저장한다() throws Exception {
        Member member = saveMemberWithRoom(false);

        create(member, imageKey(member), placedTypeIds.get(1), null, "[\" #추리 \", \"외국소설\", \"#추리\"]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags[0]").value("추리"))
                .andExpect(jsonPath("$.tags[1]").value("외국소설"));
    }

    @Test
    void 태그_없이도_기록할_수_있다() throws Exception {
        Member member = saveMemberWithRoom(false);

        create(member, imageKey(member), placedTypeIds.get(1), null, "[]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void 태그가_3개를_넘거나_비었거나_20자를_넘으면_400을_반환한다() throws Exception {
        Member member = saveMemberWithRoom(false);
        Long typeId = placedTypeIds.get(1);

        create(member, imageKey(member), typeId, null, "[\"a\", \"b\", \"c\", \"d\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        create(member, imageKey(member), typeId, null, "[\"#\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_TAG_INVALID"));
        create(member, imageKey(member), typeId, null, "[\"%s\"]".formatted("가".repeat(21)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_TAG_INVALID"));
    }

    @Test
    void 태그가_붙은_아이템을_지우면_태그도_지워진다() throws Exception {
        Member member = saveMemberWithRoom(true);
        String body = create(member, imageKey(member), fridgeTypeId, TODAY, "[\"야식\"]")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long itemId = Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        // 등록과 삭제는 실제로 다른 요청이다. 등록 때 영속성 컨텍스트에 남은 태그를 비운다
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/fridge/foods/{itemId}", itemId).header("Authorization", bearer(member)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        assertThat(itemTagRepository.findAllByItemIdOrderByPosition(itemId)).isEmpty();
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
        return create(member, key, furnitureTypeId, recordedOn, "null");
    }

    private ResultActions create(Member member, String key, Long furnitureTypeId, LocalDate recordedOn,
            String tagsJson) throws Exception {
        String recordedOnJson = recordedOn == null ? "null" : "\"" + recordedOn + "\"";
        return mockMvc.perform(post("/api/v1/items")
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageKey":"%s","furnitureTypeId":%d,"recordedOn":%s,"visibility":"PUBLIC","tags":%s}
                        """.formatted(key, furnitureTypeId, recordedOnJson, tagsJson)));
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
