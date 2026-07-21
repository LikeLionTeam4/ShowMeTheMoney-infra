package com.showmethemoney.recurringitem.application;

import com.showmethemoney.category.application.CategoryService;
import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import com.showmethemoney.recurringitem.domain.RecurringItem;
import com.showmethemoney.recurringitem.infrastructure.RecurringItemMapper;
import com.showmethemoney.recurringitem.interfaces.dto.CreateRecurringItemRequest;
import com.showmethemoney.recurringitem.interfaces.dto.RecurringItemResponse;
import com.showmethemoney.recurringitem.interfaces.dto.UpdateRecurringItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecurringItemService {

    private final RecurringItemMapper recurringItemMapper;
    private final CategoryService categoryService;

    public RecurringItemService(RecurringItemMapper recurringItemMapper, CategoryService categoryService) {
        this.recurringItemMapper = recurringItemMapper;
        this.categoryService = categoryService;
    }

    @Transactional
    public void create(Long userId, CreateRecurringItemRequest request) {
        Long uuidCategory = categoryService.validateAndGetId(request.categoryCode(), request.type());
        RecurringItem item = new RecurringItem();
        item.setUuidUser(userId);
        item.setUuidCategory(uuidCategory);
        item.setType(request.type());
        item.setName(request.name());
        item.setAmount(request.amount());
        item.setBillingDay(request.billingDay());
        recurringItemMapper.insert(item);
    }

    @Transactional(readOnly = true)
    public List<RecurringItemResponse> getList(Long userId, Boolean isActive, Integer type) {
        return recurringItemMapper.findAll(userId, isActive, type).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void update(Long userId, Long id, UpdateRecurringItemRequest request) {
        RecurringItem existing = findOwnedItem(userId, id);

        Long uuidCategory = null;
        if (request.categoryCode() != null) {
            uuidCategory = categoryService.validateAndGetId(request.categoryCode(), existing.getType());
        }

        RecurringItem update = new RecurringItem();
        update.setUuid(id);
        update.setUuidCategory(uuidCategory);
        update.setName(request.name());
        update.setAmount(request.amount());
        update.setBillingDay(request.billingDay());
        update.setIsActive(request.isActive());
        recurringItemMapper.update(update);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        findOwnedItem(userId, id);
        recurringItemMapper.softDelete(id);
    }

    private RecurringItem findOwnedItem(Long userId, Long id) {
        RecurringItem item = recurringItemMapper.findById(id);
        if (item == null || item.getDeletedAt() != null) throw new BusinessException(ErrorCode.RECURRING_ITEM_NOT_FOUND);
        if (!item.getUuidUser().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        return item;
    }

    private RecurringItemResponse toResponse(RecurringItem item) {
        String typeStr = item.getType() == 1 ? "INCOME" : "EXPENSE";
        Integer isActiveInt = Boolean.TRUE.equals(item.getIsActive()) ? 1 : 0;
        return new RecurringItemResponse(item.getUuid(), typeStr, item.getCategoryCode(),
                item.getCategoryName(), item.getName(), item.getAmount(), item.getBillingDay(), isActiveInt);
    }
}
