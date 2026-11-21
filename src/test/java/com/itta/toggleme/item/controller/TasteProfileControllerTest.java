package com.itta.toggleme.item.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemTag;
import com.itta.toggleme.item.domain.ItemVisibility;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.item.repository.ItemTagRepository;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TasteProfileControllerTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemTagRepository itemTagRepository;

    @Test
    void 취향_카드는_로그인해야_볼_수_있다() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/taste-profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 기록이_5개_미만이면_200과_eligible_false를_돌려준다() throws Exception {
        Member member = member();
        saveItem(member, "추리");

        mockMvc.perform(get("/api/v1/members/me/taste-profile").header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.requiredCount").value(5))
                .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void 기록이_5개_이상이면_기간과_태그를_돌려준다() throws Exception {
        Member member = member();
        for (int i = 0; i < 5; i++) {
            saveItem(member, "추리");
        }

        mockMvc.perform(get("/api/v1/members/me/taste-profile").header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(30))
                .andExpect(jsonPath("$.from").value(TODAY.minusDays(29).toString()))
                .andExpect(jsonPath("$.to").value(TODAY.toString()))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.tags[0].name").value("추리"))
                .andExpect(jsonPath("$.tags[0].count").value(5));
    }

    private Member member() {
        return memberRepository.save(Member.create(UUID.randomUUID().toString().substring(0, 20), "회원"));
    }

    private void saveItem(Member member, String tag) {
        FurnitureType furniture = furnitureTypeRepository.findAll().stream().findFirst()
                .orElseGet(() -> furnitureTypeRepository.save(
                        FurnitureType.create(UUID.randomUUID().toString(), "가구", 903)));
        Item item = itemRepository.save(Item.record(
                member, furniture, TODAY, "taste/" + UUID.randomUUID(), null, ItemVisibility.PUBLIC));
        itemTagRepository.save(ItemTag.of(item, tag, 0));
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }
}
