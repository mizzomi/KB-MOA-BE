package org.moa.reservation.restaurant.service;

import lombok.RequiredArgsConstructor;
import org.moa.reservation.restaurant.dto.CategoryResponseDto;
import org.moa.reservation.restaurant.type.Category;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    @Override
    public List<CategoryResponseDto> getAllCategories() {
        return Arrays.stream(Category.values())
                .map(category -> new CategoryResponseDto(
                        category.getCategoryId(),
                        category.getCategoryCode(),
                        category.getCategoryName()
                ))
                .collect(Collectors.toList());
    }
}
