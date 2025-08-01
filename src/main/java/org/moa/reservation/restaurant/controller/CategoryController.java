package org.moa.reservation.restaurant.controller;

import lombok.RequiredArgsConstructor;
import org.moa.reservation.restaurant.dto.CategoryResponseDto;
import org.moa.reservation.restaurant.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 카테고리 리스트 조회
@RestController
@RequestMapping("/api/member/reservation/restaurant")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/category")
    public List<CategoryResponseDto> getCategories() {
        return categoryService.getAllCategories();
    }
}
