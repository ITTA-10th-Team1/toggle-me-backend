package com.itta.toggleme.follow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itta.toggleme.global.security.JwtTokenProvider;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.repository.RoomRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Member me;
    private Member other;

    @BeforeEach
    void setUp() {
        me = saveMember();
        other = saveMember();
    }

    @Test
    void 팔로우하면_프로필의_팔로워_수와_팔로우_여부가_바뀐다() throws Exception {
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/members/" + other.getHandle()).header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(other.getId()))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followingCount").value(0))
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.roomPublic").value(true));
    }

    @Test
    void 같은_회원을_두_번_팔로우해도_한_번만_센다() throws Exception {
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/members/" + other.getHandle() + "/followers").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].handle").value(me.getHandle()));
    }

    @Test
    void 언팔로우하면_목록에서_빠지고_다시_해도_성공한다() throws Exception {
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)));

        mockMvc.perform(delete(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/members/" + me.getHandle() + "/following").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 고유_ID에_대문자가_섞여도_찾는다() throws Exception {
        mockMvc.perform(put("/api/v1/members/" + other.getHandle().toUpperCase() + "/follow")
                        .header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/members/" + other.getHandle().toUpperCase()).header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handle").value(other.getHandle()))
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void 자기_자신은_팔로우할_수_없다() throws Exception {
        mockMvc.perform(put(followPath(me)).header("Authorization", bearer(me)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FOLLOW_SELF"));
    }

    @Test
    void 없는_회원은_팔로우할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/members/nobody_" + randomSuffix() + "/follow").header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());
    }

    @Test
    void 방이_OFF인_회원은_다른_사람에게_보이지_않고_다시_ON이면_팔로우가_돌아온다() throws Exception {
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)));
        changeRoom(other, RoomVisibility.SOCIAL_OFF);

        mockMvc.perform(get("/api/v1/members/" + me.getHandle() + "/following").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/members/" + me.getHandle()).header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.followingCount").value(0));
        mockMvc.perform(get("/api/v1/members/" + other.getHandle()).header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());

        changeRoom(other, RoomVisibility.SOCIAL_ON);

        mockMvc.perform(get("/api/v1/members/" + me.getHandle() + "/following").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].handle").value(other.getHandle()));
    }

    @Test
    void 방이_OFF여도_본인은_다른_사람을_팔로우하고_내_프로필을_본다() throws Exception {
        changeRoom(me, RoomVisibility.SOCIAL_OFF);

        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/members/" + me.getHandle()).header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followingCount").value(1))
                .andExpect(jsonPath("$.roomPublic").value(false));
        mockMvc.perform(get("/api/v1/members/" + other.getHandle()).header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void 탈퇴한_회원은_목록과_수에서_빠진다() throws Exception {
        Member withdrawn = saveMember();
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(me)));
        mockMvc.perform(put(followPath(other)).header("Authorization", bearer(withdrawn)));
        jdbcTemplate.update("UPDATE member SET status = 'WITHDRAWN' WHERE id = ?", withdrawn.getId());

        mockMvc.perform(get("/api/v1/members/" + other.getHandle()).header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.followerCount").value(1));
        mockMvc.perform(get("/api/v1/members/" + other.getHandle() + "/followers").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/members/" + withdrawn.getHandle()).header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());
    }

    private Member saveMember() {
        Member member = memberRepository.save(Member.create("user_" + randomSuffix(), "토글러"));
        member.updateProfile("member_" + randomSuffix(), "토글러");
        roomRepository.save(Room.create(member, "방", RoomVisibility.SOCIAL_ON));
        return member;
    }

    private void changeRoom(Member member, RoomVisibility visibility) {
        roomRepository.findFirstByOwnerIdOrderById(member.getId()).orElseThrow().changeVisibility(visibility);
    }

    private String followPath(Member target) {
        return "/api/v1/members/" + target.getHandle() + "/follow";
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
