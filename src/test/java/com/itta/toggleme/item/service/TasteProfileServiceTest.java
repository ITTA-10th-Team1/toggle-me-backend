package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemTag;
import com.itta.toggleme.item.domain.ItemVisibility;
import com.itta.toggleme.item.dto.TasteProfileResponse;
import com.itta.toggleme.item.dto.TasteTagResponse;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.item.repository.ItemTagRepository;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// 집계 쿼리를 실제 DB에서 확인한다. 회원을 새로 만들어 쓰므로 로컬 DB에 다른 데이터가 있어도 된다
@SpringBootTest
@Transactional
class TasteProfileServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    @Autowired
    private TasteProfileService service;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemTagRepository itemTagRepository;

    private Member me;
    private FurnitureType fridge;
    private FurnitureType bookshelf;

    @BeforeEach
    void setUp() {
        me = member();
        fridge = furnitureTypeRepository.save(FurnitureType.create(UUID.randomUUID().toString(), "냉장고", 901));
        bookshelf = furnitureTypeRepository.save(FurnitureType.create(UUID.randomUUID().toString(), "책장", 902));
    }

    @Test
    void 기간은_오늘을_포함한_30일이고_29일_전은_포함_30일_전은_제외한다() {
        for (int i = 0; i < 4; i++) {
            record(me, fridge, TODAY, ItemVisibility.PUBLIC, "오늘");
        }
        record(me, fridge, TODAY.minusDays(29), ItemVisibility.PUBLIC, "경계안");
        record(me, fridge, TODAY.minusDays(30), ItemVisibility.PUBLIC, "경계밖");
        record(me, fridge, TODAY.plusDays(1), ItemVisibility.PUBLIC, "미래");

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.periodDays()).isEqualTo(30);
        assertThat(profile.from()).isEqualTo(TODAY.minusDays(29));
        assertThat(profile.to()).isEqualTo(TODAY);
        assertThat(profile.itemCount()).isEqualTo(5);
        assertThat(profile.eligible()).isTrue();
        assertThat(names(profile)).containsExactlyInAnyOrder("오늘", "경계안");
    }

    @Test
    void 비공개_기록은_개수와_태그에서_제외한다() {
        for (int i = 0; i < 5; i++) {
            record(me, fridge, TODAY, ItemVisibility.PUBLIC, "공개");
        }
        record(me, fridge, TODAY, ItemVisibility.PRIVATE, "비공개");

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.itemCount()).isEqualTo(5);
        assertThat(names(profile)).containsExactly("공개");
    }

    @Test
    void 공개_기록이_5개_미만이면_eligible_false이고_태그는_비어_있다() {
        for (int i = 0; i < 4; i++) {
            record(me, fridge, TODAY, ItemVisibility.PUBLIC, "추리");
        }
        record(me, fridge, TODAY, ItemVisibility.PRIVATE, "추리");

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.eligible()).isFalse();
        assertThat(profile.itemCount()).isEqualTo(4);
        assertThat(profile.requiredCount()).isEqualTo(5);
        assertThat(profile.tags()).isEmpty();
    }

    @Test
    void 태그가_없는_기록도_기록_수에_센다() {
        for (int i = 0; i < 5; i++) {
            record(me, fridge, TODAY, ItemVisibility.PUBLIC);
        }

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.eligible()).isTrue();
        assertThat(profile.itemCount()).isEqualTo(5);
        assertThat(profile.tags()).isEmpty();
    }

    @Test
    void 태그는_많이_나온_순이고_동점이면_이름_순이며_가구를_합쳐서_센다() {
        record(me, fridge, TODAY, ItemVisibility.PUBLIC, "파스타", "추리");
        record(me, bookshelf, TODAY, ItemVisibility.PUBLIC, "추리", "가나");
        record(me, bookshelf, TODAY, ItemVisibility.PUBLIC, "추리", "나다");
        record(me, fridge, TODAY, ItemVisibility.PUBLIC, "파스타");
        record(me, fridge, TODAY, ItemVisibility.PUBLIC, "다라");

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.tags()).containsExactly(
                new TasteTagResponse("추리", 3),
                new TasteTagResponse("파스타", 2),
                new TasteTagResponse("가나", 1),
                new TasteTagResponse("나다", 1),
                new TasteTagResponse("다라", 1));
    }

    @Test
    void 태그는_최대_12개까지만_돌려준다() {
        for (int i = 0; i < 15; i++) {
            record(me, fridge, TODAY, ItemVisibility.PUBLIC, "태그%02d".formatted(i));
        }

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.itemCount()).isEqualTo(15);
        assertThat(profile.tags()).hasSize(12);
        assertThat(profile.tags().get(0).name()).isEqualTo("태그00");
    }

    @Test
    void 남의_기록은_세지_않는다() {
        Member other = member();
        for (int i = 0; i < 6; i++) {
            record(other, fridge, TODAY, ItemVisibility.PUBLIC, "남의태그");
        }
        record(me, fridge, TODAY, ItemVisibility.PUBLIC, "내태그");

        TasteProfileResponse profile = service.getProfile(me.getId(), TODAY);

        assertThat(profile.itemCount()).isEqualTo(1);
        assertThat(profile.eligible()).isFalse();
        assertThat(profile.tags()).isEmpty();
        assertThat(names(service.getProfile(other.getId(), TODAY))).containsExactly("남의태그");
    }

    private Member member() {
        return memberRepository.save(Member.create(UUID.randomUUID().toString().substring(0, 20), "회원"));
    }

    private void record(Member member, FurnitureType furniture, LocalDate recordedOn, ItemVisibility visibility,
                        String... tags) {
        Item item = itemRepository.save(Item.record(
                member, furniture, recordedOn, "taste/" + UUID.randomUUID(), null, visibility));
        for (int i = 0; i < tags.length; i++) {
            itemTagRepository.save(ItemTag.of(item, tags[i], i));
        }
    }

    private static List<String> names(TasteProfileResponse profile) {
        return profile.tags().stream().map(TasteTagResponse::name).toList();
    }
}
