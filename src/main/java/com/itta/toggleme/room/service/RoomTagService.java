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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방 태그: 가구별 태그를 돌아가며 뽑아 방 전체 태그를 최대 {@value #MAX_TAGS}개 보여준다.
 *
 * <ol>
 *   <li>대상: 공개 아이템이면서 방에 켜진 가구에 기록한 것. 내 방과 남이 보는 방에 같은 결과를 쓴다</li>
 *   <li>가구 안 순위: 태그가 붙은 아이템 수 많은 순 → 그 태그가 붙은 최근 아이템 순 → 그 아이템에서 앞 순서</li>
 *   <li>가구 순서: 최근에 기록한 가구부터. 한 바퀴에 가구마다 하나씩 고르고, 첫 바퀴가 가구별 대표 태그다</li>
 *   <li>앞에서 이미 고른 태그는 건너뛰고 다음 순위를 쓴다. 고를 태그가 없는 가구는 빠진다</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTagService {

    static final int MAX_TAGS = 5;

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

        Map<Long, Iterator<String>> ranked = new LinkedHashMap<>();
        furnitureOrder.forEach(id -> ranked.put(id, rankTags(byFurniture.get(id)).iterator()));

        // 가구 순서대로 한 바퀴에 하나씩 돌아가며 고른다. 첫 바퀴가 가구별 대표 태그다
        Set<String> picked = new HashSet<>();
        List<RoomTagResponse> roomTags = new ArrayList<>();
        while (roomTags.size() < MAX_TAGS && !ranked.isEmpty()) {
            for (var it = ranked.entrySet().iterator(); it.hasNext() && roomTags.size() < MAX_TAGS; ) {
                var entry = it.next();
                Optional<String> next = nextUnpicked(entry.getValue(), picked);
                if (next.isPresent()) {
                    roomTags.add(new RoomTagResponse(entry.getKey(), next.get()));
                } else {
                    it.remove();
                }
            }
        }
        return roomTags;
    }

    private static Optional<String> nextUnpicked(Iterator<String> tags, Set<String> picked) {
        while (tags.hasNext()) {
            String name = tags.next();
            if (picked.add(name)) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
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
