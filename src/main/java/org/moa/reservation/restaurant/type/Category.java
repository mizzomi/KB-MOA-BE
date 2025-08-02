package org.moa.reservation.restaurant.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    KOREAN(1, "korean", "한식"),
    CHINESE(2, "chinese", "중식"),
    JAPANESE(3, "japanese", "일식"),
    WESTERN(4, "western", "양식"),
    ETC(5, "etc", "기타");

    private final int categoryId;
    private final String categoryCode;
    private final String categoryName;
}