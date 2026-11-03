package com.itta.toggleme.item.service.handler;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.item.exception.FridgeNotSelectedException;
import org.springframework.stereotype.Component;

// 냉장고는 공통 규칙만 쓴다. 앱이 이미 의존하는 오류 코드(FRIDGE_NOT_SELECTED)를 유지하려고 남겨 둔다
@Component
public class FridgeItemHandler implements ItemRecordHandler {

    public static final String FRIDGE_CODE = "fridge";

    @Override
    public String furnitureCode() {
        return FRIDGE_CODE;
    }

    @Override
    public BusinessException furnitureNotSelected() {
        return new FridgeNotSelectedException();
    }
}
