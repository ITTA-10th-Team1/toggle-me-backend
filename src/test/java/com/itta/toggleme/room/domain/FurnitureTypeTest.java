package com.itta.toggleme.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FurnitureTypeTest {

    @Test
    void 배치_값_10개가_모두_있어야_배치가_설정된_것이다() {
        FurnitureType complete = FurnitureType.create("fridge", "냉장고", 1);
        complete.configurePlacement(Placement.at(1, 0, 2), 1);

        FurnitureType missingScale = FurnitureType.create("mirror", "거울", 2);
        missingScale.configurePlacement(new Placement(1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, null), 2);

        assertThat(complete.hasConfiguredPlacement()).isTrue();
        assertThat(missingScale.hasConfiguredPlacement()).isFalse();
    }

    @Test
    void 배치나_그리는_순서가_없으면_설정되지_않은_것이다() {
        assertThat(FurnitureType.create("tv", "TV", 3).hasConfiguredPlacement()).isFalse();
    }
}
