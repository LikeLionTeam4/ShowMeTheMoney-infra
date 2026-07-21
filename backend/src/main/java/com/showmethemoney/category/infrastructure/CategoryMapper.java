package com.showmethemoney.category.infrastructure;

import com.showmethemoney.category.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<Category> findAll(Integer type);

    Category findByCodeAndType(@Param("code") String code, @Param("type") Integer type);
}
