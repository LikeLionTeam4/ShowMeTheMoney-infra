package com.showmethemoney.category.application;

import com.showmethemoney.category.domain.Category;
import com.showmethemoney.category.infrastructure.CategoryMapper;
import com.showmethemoney.category.interfaces.dto.CategoryResponse;
import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Integer type) {
        return categoryMapper.findAll(type).stream()
                .map(c -> new CategoryResponse(c.getCode(), c.getCodeNumber(), c.getName(), c.getType()))
                .toList();
    }

    // 다른 도메인의 Service가 카테고리 검증 시 사용하는 내부 API — categoryId만 반환해 도메인 객체 노출 방지
    @Transactional(readOnly = true)
    public Long validateAndGetId(String code, Integer type) {
        Category category = categoryMapper.findByCodeAndType(code, type);
        if (category == null) throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        return category.getUuid();
    }
}
