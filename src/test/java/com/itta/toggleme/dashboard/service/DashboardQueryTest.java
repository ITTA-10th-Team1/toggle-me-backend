package com.itta.toggleme.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itta.toggleme.dashboard.dto.DashboardResponse;
import com.itta.toggleme.image.domain.ImageAnalysis;
import com.itta.toggleme.image.domain.ImageAnalysisResult;
import com.itta.toggleme.image.repository.ImageAnalysisRepository;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.domain.ItemVisibility;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.FurnitureType;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// 집계 쿼리를 실제 DB에서 확인한다. 로컬 DB에 다른 데이터가 있어도 되게, 데이터를 넣기 전후의 차이를 본다
@SpringBootTest
@Transactional
class DashboardQueryTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FurnitureTypeRepository furnitureTypeRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ImageAnalysisRepository imageAnalysisRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 퍼널_AI_분석_가구별_기록을_DB에서_집계한다() {
        DashboardResponse before = dashboardService.getDashboard(7);

        FurnitureType furniture = furnitureTypeRepository.save(
                FurnitureType.create(UUID.randomUUID().toString(), "대시보드 가구", 999));
        Member returned = member(true);
        Member recordedOnce = member(true);
        member(false);
        LocalDate today = LocalDate.now();
        record(returned, furniture, today);
        record(returned, furniture, today.minusDays(1));
        record(recordedOnce, furniture, today);
        record(recordedOnce, furniture, today);

        ImageAnalysis completed = imageAnalysisRepository.save(ImageAnalysis.request(returned, key(), furniture));
        completed.complete(new ImageAnalysisResult(List.of("태그"), List.of()));
        imageAnalysisRepository.save(ImageAnalysis.request(returned, key(), furniture));
        ImageAnalysis stuck = imageAnalysisRepository.save(ImageAnalysis.request(returned, key(), furniture));
        imageAnalysisRepository.flush();
        jdbcTemplate.update("update image_analysis set created_at = date_sub(created_at, interval 2 minute) where id = ?",
                stuck.getId());

        DashboardResponse after = dashboardService.getDashboard(7);

        assertThat(after.funnel().signedUp() - before.funnel().signedUp()).isEqualTo(3);
        assertThat(after.funnel().profileCompleted() - before.funnel().profileCompleted()).isEqualTo(2);
        assertThat(after.funnel().recorded() - before.funnel().recorded()).isEqualTo(2);
        assertThat(after.funnel().returned() - before.funnel().returned()).isEqualTo(1);

        assertThat(after.imageAnalysis().total() - before.imageAnalysis().total()).isEqualTo(3);
        assertThat(after.imageAnalysis().completed() - before.imageAnalysis().completed()).isEqualTo(1);
        assertThat(after.imageAnalysis().pending() - before.imageAnalysis().pending()).isEqualTo(1);
        assertThat(after.imageAnalysis().stuck() - before.imageAnalysis().stuck()).isEqualTo(1);
        assertThat(after.imageAnalysis().avgSeconds()).isNotNull();

        assertThat(after.furnitureItems())
                .filteredOn(row -> row.code().equals(furniture.getCode()))
                .extracting("name", "items")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("대시보드 가구", 4L));
    }

    private Member member(boolean profileCompleted) {
        Member member = Member.create(UUID.randomUUID().toString().substring(0, 20), "회원");
        if (profileCompleted) {
            member.updateProfile(member.getHandle(), "회원");
        }
        return memberRepository.save(member);
    }

    private void record(Member member, FurnitureType furniture, LocalDate recordedOn) {
        itemRepository.save(Item.record(member, furniture, recordedOn, key(), null, ItemVisibility.PRIVATE));
    }

    private static String key() {
        return "dashboard/" + UUID.randomUUID();
    }
}
