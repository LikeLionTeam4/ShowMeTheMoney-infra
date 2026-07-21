package com.showmethemoney.category.interfaces;

import com.showmethemoney.category.application.CategoryService;
import com.showmethemoney.category.interfaces.dto.CategoryResponse;
import com.showmethemoney.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories(
            @RequestParam(name = "type", required = false) Integer type) {
        return ApiResponse.ok(categoryService.getCategories(type));
    }
}
