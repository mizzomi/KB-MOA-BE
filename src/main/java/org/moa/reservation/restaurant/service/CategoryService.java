package org.moa.reservation.restaurant.service;

import org.moa.reservation.restaurant.dto.CategoryResponseDto;
import java.util.List;

public interface CategoryService {
    List<CategoryResponseDto> getAllCategories();
}