package com.itta.toggleme.item.service.handler;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.item.dto.ItemCreateRequest;
import com.itta.toggleme.item.exception.ItemFurnitureNotSelectedException;
import com.itta.toggleme.room.domain.FurnitureType;

/**
 * 가구별 아이템 등록 규칙. 거울처럼 공통 규칙만 쓰는 가구는 핸들러가 필요 없다.
 * 가구에 고유 규칙이 생기면(예: 책은 도서 정보 필수) 이 인터페이스를 구현한 빈을 하나 추가한다.
 *
 * <p>공통 규칙(ItemService): 회원 잠금, 방에 켜진 가구인지, 등록일은 서비스 기준 오늘로 자동 기록(PRD ARCH-05),
 * 이미지가 본인 업로드이고 아직 쓰이지 않았는지, 태그 0~3개, 가구별 하루 등록 개수 제한(ItemDailyLimit, 예: 냉장고 하루 1장).
 * 개수 제한은 핸들러가 아니라 그 표에서 관리한다.
 */
public interface ItemRecordHandler {

    /** 처리할 가구의 {@link FurnitureType#getCode()}. */
    String furnitureCode();

    /** 가구별 규칙을 검사한다. 공통 규칙을 통과한 뒤, 같은 트랜잭션에서 회원 행을 잠근 상태로 호출된다. */
    default void validate(Long memberId, FurnitureType furnitureType, ItemCreateRequest request) {
    }

    /** 방에 이 가구가 없을 때 던질 예외. 가구 전용 오류 코드가 있으면 바꾼다. */
    default BusinessException furnitureNotSelected() {
        return new ItemFurnitureNotSelectedException();
    }
}
