package org.moa.reservation.restaurant.controller;

import lombok.RequiredArgsConstructor;
import org.moa.global.response.ApiResponse;
import org.moa.reservation.restaurant.dto.CategoryResponseDto;
import org.moa.reservation.restaurant.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/member/reservation/restaurant")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/category")
    public ApiResponse<List<CategoryResponseDto>> getCategories() {
        List<CategoryResponseDto> categories = categoryService.getAllCategories();

        return ApiResponse.of(categories);
    }
}
