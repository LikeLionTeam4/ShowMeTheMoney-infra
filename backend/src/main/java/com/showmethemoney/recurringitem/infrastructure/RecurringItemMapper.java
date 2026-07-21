package com.showmethemoney.recurringitem.infrastructure;

import com.showmethemoney.recurringitem.domain.RecurringItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RecurringItemMapper {

    void insert(RecurringItem item);

    List<RecurringItem> findAll(Long userId, Boolean isActive, Integer type);

    RecurringItem findById(Long id);

    void update(RecurringItem item);

    void softDelete(Long id);
}
