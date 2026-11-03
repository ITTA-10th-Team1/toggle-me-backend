package com.itta.toggleme.follow.controller;

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
class MemberSearchControllerTest {

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

    // 다른 테스트 데이터와 겹치지 않는 고유 ID 앞부분
    private String prefix;
    private Member me;

    @BeforeEach
    void setUp() {
        prefix = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 7);
        me = saveMember("me" + prefix, RoomVisibility.SOCIAL_ON);
    }

    @Test
    void 고유_ID_앞부분으로_찾고_팔로우_여부를_함께_준다() throws Exception {
        Member followed = saveMember(prefix + "b", RoomVisibility.SOCIAL_ON);
        saveMember(prefix + "a", RoomVisibility.SOCIAL_ON);
        saveMember("x" + prefix, RoomVisibility.SOCIAL_ON);
        mockMvc.perform(put("/api/v1/members/" + followed.getHandle() + "/follow").header("Authorization", bearer(me)));

        mockMvc.perform(get("/api/v1/members/search").param("keyword", " " + prefix.toUpperCase() + " ")
                        .header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].handle").value(prefix + "a"))
                .andExpect(jsonPath("$[0].following").value(false))
                .andExpect(jsonPath("$[1].handle").value(prefix + "b"))
                .andExpect(jsonPath("$[1].following").value(true));
    }

    @Test
    void 밑줄은_글자_그대로_찾는다() throws Exception {
        saveMember(prefix + "_a", RoomVisibility.SOCIAL_ON);
        saveMember(prefix + "xa", RoomVisibility.SOCIAL_ON);

        mockMvc.perform(get("/api/v1/members/search").param("keyword", prefix + "_").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].handle").value(prefix + "_a"));
    }

    @Test
    void 방이_OFF이거나_탈퇴한_회원은_빠지고_나는_OFF여도_나온다() throws Exception {
        saveMember(prefix + "off", RoomVisibility.SOCIAL_OFF);
        Member withdrawn = saveMember(prefix + "bye", RoomVisibility.SOCIAL_ON);
        // 엔티티 변경이 나중에 flush되면 JDBC로 바꾼 상태를 덮어쓰므로 먼저 반영한다
        memberRepository.flush();
        jdbcTemplate.update("UPDATE member SET status = 'WITHDRAWN' WHERE id = ?", withdrawn.getId());
        Member offMe = saveMember(prefix + "me", RoomVisibility.SOCIAL_OFF);

        mockMvc.perform(get("/api/v1/members/search").param("keyword", prefix).header("Authorization", bearer(offMe)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].handle").value(prefix + "me"));
    }

    @Test
    void 닉네임_앞부분으로도_찾는다() throws Exception {
        saveMember("zz" + prefix, "지인" + prefix, RoomVisibility.SOCIAL_ON);
        saveMember("zy" + prefix, "Jiin" + prefix, RoomVisibility.SOCIAL_ON);

        mockMvc.perform(get("/api/v1/members/search").param("keyword", "지인" + prefix).header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].handle").value("zz" + prefix))
                .andExpect(jsonPath("$[0].nickname").value("지인" + prefix));
        mockMvc.perform(get("/api/v1/members/search").param("keyword", "JIIN" + prefix).header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].handle").value("zy" + prefix));
    }

    @Test
    void 닉네임의_퍼센트는_글자_그대로_찾는다() throws Exception {
        saveMember("zz" + prefix, prefix + "%a", RoomVisibility.SOCIAL_ON);
        saveMember("zy" + prefix, prefix + "xa", RoomVisibility.SOCIAL_ON);

        mockMvc.perform(get("/api/v1/members/search").param("keyword", prefix + "%").header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nickname").value(prefix + "%a"));
    }

    @Test
    void 두_글자_미만이거나_30자를_넘으면_빈_목록이다() throws Exception {
        mockMvc.perform(get("/api/v1/members/search").param("keyword", "s").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/members/search").param("keyword", "a".repeat(31)).header("Authorization", bearer(me)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Member saveMember(String handle, RoomVisibility visibility) {
        return saveMember(handle, "토글러", visibility);
    }

    private Member saveMember(String handle, String nickname, RoomVisibility visibility) {
        Member member = memberRepository.save(Member.create("user_" + UUID.randomUUID().toString().substring(0, 8), "토글러"));
        member.updateProfile(handle, nickname);
        roomRepository.save(Room.create(member, "방", visibility));
        return member;
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    }
}
