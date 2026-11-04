package com.itta.toggleme.item.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.itta.toggleme.image.service.ImageService;
import com.itta.toggleme.item.domain.Item;
import com.itta.toggleme.item.repository.ItemRepository;
import com.itta.toggleme.item.repository.ItemTagRepository;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.repository.FurnitureTypeRepository;
import com.itta.toggleme.room.repository.RoomFurnitureRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 사진 삭제는 DB 커밋 뒤(afterCommit)에 실행된다. 롤백되는 통합 테스트에서는 돌지 않아 여기서 직접 호출한다
class ItemServiceDeleteTest {

    private static final String IMAGE_KEY = "images/7/a.jpg";

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final ImageService imageService = mock(ImageService.class);
    private final ItemService itemService = new ItemService(
            itemRepository,
            mock(ItemTagRepository.class),
            mock(MemberRepository.class),
            mock(FurnitureTypeRepository.class),
            mock(RoomFurnitureRepository.class),
            imageService,
            List.of()
    );

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        Item item = mock(Item.class);
        given(item.getImageKey()).willReturn(IMAGE_KEY);
        given(itemRepository.findByIdAndMemberId(1L, 7L)).willReturn(Optional.of(item));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void 커밋되면_사진도_지운다() {
        itemService.delete(7L, 1L);

        runAfterCommit();

        verify(imageService).delete(IMAGE_KEY);
    }

    @Test
    void 사진_삭제가_실패해도_예외를_던지지_않는다() {
        willThrow(new IllegalStateException("S3 down")).given(imageService).delete(any());
        itemService.delete(7L, 1L);

        assertThatCode(this::runAfterCommit).doesNotThrowAnyException();
    }

    private void runAfterCommit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }
}
