package com.itta.toggleme.item.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class FridgeFoodControllerTest {

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

    private Member owner;
    private Long fridgeVariantId;
    private List<Long> otherVariantIds;

    @BeforeEach
    void setUp() {
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.WALL, "기본 벽", "room/wall/default.glb"));
        roomStyleRepository.save(RoomStyle.create(RoomStyleType.FLOOR, "기본 바닥", "room/floor/default.glb"));
        saveCatalog();
        owner = saveMemberWithRoom(true);
        given(imageService.isUploadedBy(anyLong(), anyString())).willReturn(true);
        given(imageService.createViewUrl(anyString())).willAnswer(call -> "https://view/" + call.getArgument(0));
    }

    @Test
    void 냉장고_사진을_등록한다() throws Exception {
        String key = imageKey(owner);

        create(owner, key, TODAY, "점심")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordedOn").value(TODAY.toString()))
                .andExpect(jsonPath("$.imageUrl").value("https://view/" + key))
                .andExpect(jsonPath("$.memo").value("점심"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    void 지난_날짜로도_등록할_수_있다() throws Exception {
        create(owner, imageKey(owner), TODAY.minusDays(3), null)
                .andExpect(status().isCreated());
    }

    @Test
    void 미래_날짜는_등록할_수_없다() throws Exception {
        create(owner, imageKey(owner), TODAY.plusDays(1), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_RECORDED_DATE_INVALID"));
    }

    @Test
    void 방에_냉장고가_없으면_등록할_수_없다() throws Exception {
        Member withoutFridge = saveMemberWithRoom(false);

        create(withoutFridge, imageKey(withoutFridge), TODAY, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FRIDGE_NOT_SELECTED"));
    }

    @Test
    void 하루_10장을_넘게_등록할_수_없다() throws Exception {
        for (int i = 0; i < 10; i++) {
            create(owner, imageKey(owner), TODAY, null).andExpect(status().isCreated());
        }

        create(owner, imageKey(owner), TODAY, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ITEM_DAILY_LIMIT_EXCEEDED"));
        create(owner, imageKey(owner), TODAY.minusDays(1), null)
                .andExpect(status().isCreated());
    }

    @Test
    void 업로드되지_않은_이미지나_이미_쓴_이미지는_등록할_수_없다() throws Exception {
        String missingKey = imageKey(owner);
        given(imageService.isUploadedBy(owner.getId(), missingKey)).willReturn(false);
        create(owner, missingKey, TODAY, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_IMAGE_INVALID"));

        String usedKey = imageKey(owner);
        create(owner, usedKey, TODAY, null).andExpect(status().isCreated());
        create(owner, usedKey, TODAY, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_IMAGE_INVALID"));
    }

    @Test
    void 공개_여부가_없거나_메모가_200자를_넘으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/fridge/foods")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageKey":"%s","recordedOn":"%s","memo":"%s"}
                                """.formatted(imageKey(owner), TODAY, "가".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void 날짜별_목록과_달력을_조회한다() throws Exception {
        LocalDate otherDay = TODAY.withDayOfMonth(1).equals(TODAY) ? TODAY : TODAY.minusDays(1);
        String firstKey = imageKey(owner);
        create(owner, firstKey, TODAY, "아침").andExpect(status().isCreated());
        create(owner, imageKey(owner), TODAY, "저녁").andExpect(status().isCreated());
        if (!otherDay.equals(TODAY)) {
            create(owner, imageKey(owner), otherDay, null).andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/fridge/foods").param("date", TODAY.toString())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].memo").value("아침"))
                .andExpect(jsonPath("$[1].memo").value("저녁"));

        mockMvc.perform(get("/api/v1/fridge/foods/calendar").param("month", TODAY.toString().substring(0, 7))
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[-1:].date").value(TODAY.toString()))
                .andExpect(jsonPath("$[-1:].count").value(2))
                .andExpect(jsonPath("$[-1:].thumbnailUrl").value("https://view/" + firstKey));
    }

    @Test
    void 메모와_공개_여부를_수정한다() throws Exception {
        Long itemId = createdId(owner);

        mockMvc.perform(patch("/api/v1/fridge/foods/{itemId}", itemId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"야식\",\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memo").value("야식"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void 사진을_삭제한다() throws Exception {
        Long itemId = createdId(owner);

        mockMvc.perform(delete("/api/v1/fridge/foods/{itemId}", itemId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/fridge/foods").param("date", TODAY.toString())
                        .header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 다른_회원의_사진은_수정하거나_삭제할_수_없다() throws Exception {
        Long itemId = createdId(owner);
        Member other = saveMemberWithRoom(true);

        mockMvc.perform(patch("/api/v1/fridge/foods/{itemId}", itemId)
                        .header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/fridge/foods/{itemId}", itemId).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
    }

    private ResultActions create(Member member, String key, LocalDate date, String memo) throws Exception {
        String memoJson = memo == null ? "null" : "\"" + memo + "\"";
        return mockMvc.perform(post("/api/v1/fridge/foods")
                .header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"imageKey":"%s","recordedOn":"%s","memo":%s,"visibility":"PUBLIC"}
                        """.formatted(key, date, memoJson)));
    }

    private Long createdId(Member member) throws Exception {
        String body = create(member, imageKey(member), TODAY, null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private String imageKey(Member member) {
        return "images/" + member.getId() + "/" + UUID.randomUUID() + ".jpg";
    }

    private Member saveMemberWithRoom(boolean withFridge) {
        String handle = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Member member = memberRepository.save(Member.create(handle, "토글러"));
        List<Long> variantIds = new ArrayList<>(otherVariantIds);
        if (withFridge) {
            variantIds.set(0, fridgeVariantId);
        }
        roomService.createDefaultRoom(member, variantIds, RoomVisibility.SOCIAL_ON);
        return member;
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    // 가구 4개를 고르는 방을 만든다. 냉장고가 있는 방은 첫 가구를 냉장고로 바꾼다
    private void saveCatalog() {
        FurnitureType fridge = furnitureTypeRepository.findByCode("FRIDGE")
                .orElseGet(() -> furnitureTypeRepository.save(FurnitureType.create("FRIDGE", "냉장고", 99)));
        fridge.configurePlacement(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, 99);
        fridgeVariantId = furnitureVariantRepository.save(
                FurnitureVariant.create(fridge, "기본 냉장고", UUID.randomUUID().toString(), 1, 1)).getId();

        otherVariantIds = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            FurnitureType type = FurnitureType.create(UUID.randomUUID().toString(), "가구 " + i, i);
            type.configurePlacement(BigDecimal.valueOf(i), BigDecimal.ZERO, BigDecimal.ZERO, i);
            furnitureTypeRepository.save(type);
            otherVariantIds.add(furnitureVariantRepository.save(
                    FurnitureVariant.create(type, "기본 " + i, UUID.randomUUID().toString(), 1, 1)).getId());
        }
    }
}
