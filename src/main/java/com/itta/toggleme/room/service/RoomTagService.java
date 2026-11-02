package com.itta.toggleme.room.service;

import com.itta.toggleme.item.domain.ItemVisibility;
import com.itta.toggleme.item.repository.ItemTagRepository;
import com.itta.toggleme.item.repository.RoomTagCandidate;
import com.itta.toggleme.room.domain.Room;
import com.itta.toggleme.room.domain.RoomStatus;
import com.itta.toggleme.room.dto.RoomTagResponse;
import com.itta.toggleme.room.exception.RoomNotFoundException;
import com.itta.toggleme.room.repository.RoomRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 태그: 가구마다 대표 태그 하나를 뽑아 방 전체의 태그로 보여준다.
 *
 * <ol>
 *   <li>대상: 공개 아이템이면서 방에 켜진 가구에 기록한 것. 내 방과 남이 보는 방에 같은 결과를 쓴다</li>
 *   <li>가구 안 순위: 태그가 붙은 아이템 수 많은 순 → 그 태그가 붙은 최근 아이템 순 → 그 아이템에서 앞 순서</li>
 *   <li>가구 순서: 최근에 기록한 가구부터 고른다</li>
 *   <li>앞 가구가 이미 고른 태그는 건너뛰고 다음 순위를 쓴다. 고를 태그가 없는 가구는 빠진다</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTagService {

    private static final Comparator<RoomTagCandidate> RECENT_FIRST = Comparator
            .comparing(RoomTagCandidate::itemCreatedAt)
            .thenComparing(RoomTagCandidate::itemId)
            .reversed();

    private final RoomRepository roomRepository;
    private final ItemTagRepository itemTagRepository;

    public List<RoomTagResponse> getMyRoomTags(Long memberId, Long roomId) {
        Room room = roomRepository.findByIdAndOwnerIdAndStatus(roomId, memberId, RoomStatus.ACTIVE)
                .orElseThrow(RoomNotFoundException::new);
        return selectRoomTags(itemTagRepository.findRoomTagCandidates(
                room.getId(), memberId, ItemVisibility.PUBLIC));
    }

    static List<RoomTagResponse> selectRoomTags(List<RoomTagCandidate> candidates) {
        Map<Long, List<RoomTagCandidate>> byFurniture = candidates.stream()
                .collect(Collectors.groupingBy(RoomTagCandidate::furnitureTypeId));
        List<Long> furnitureOrder = byFurniture.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                        Comparator.comparing(rows -> rows.stream().sorted(RECENT_FIRST).findFirst().orElseThrow(),
                                RECENT_FIRST)))
                .map(Map.Entry::getKey)
                .toList();

        Set<String> picked = new HashSet<>();
        List<RoomTagResponse> roomTags = new ArrayList<>();
        for (Long furnitureTypeId : furnitureOrder) {
            rankTags(byFurniture.get(furnitureTypeId)).stream()
                    .filter(picked::add)
                    .findFirst()
                    .ifPresent(name -> roomTags.add(new RoomTagResponse(furnitureTypeId, name)));
        }
        return roomTags;
    }

    // 한 가구 안에서 태그 이름을 순위대로 나열한다
    private static List<String> rankTags(List<RoomTagCandidate> rows) {
        record Rank(String name, int count, RoomTagCandidate latest) {
        }
        return rows.stream()
                .collect(Collectors.groupingBy(RoomTagCandidate::name))
                .entrySet().stream()
                .map(entry -> new Rank(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().sorted(RECENT_FIRST).findFirst().orElseThrow()))
                .sorted(Comparator.comparing(Rank::count).reversed()
                        .thenComparing(Rank::latest, RECENT_FIRST)
                        .thenComparing(rank -> rank.latest().position()))
                .map(Rank::name)
                .toList();
    }
}
