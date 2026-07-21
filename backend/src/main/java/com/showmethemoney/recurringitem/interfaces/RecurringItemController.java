package com.showmethemoney.recurringitem.interfaces;

import com.showmethemoney.common.ApiResponse;
import com.showmethemoney.recurringitem.application.RecurringItemService;
import com.showmethemoney.recurringitem.interfaces.dto.CreateRecurringItemRequest;
import com.showmethemoney.recurringitem.interfaces.dto.RecurringItemResponse;
import com.showmethemoney.recurringitem.interfaces.dto.UpdateRecurringItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-items")
public class RecurringItemController {

    private final RecurringItemService recurringItemService;

    public RecurringItemController(RecurringItemService recurringItemService) {
        this.recurringItemService = recurringItemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody CreateRecurringItemRequest request) {
        recurringItemService.create(userId, request);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<RecurringItemResponse>> getList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "type", required = false) Integer type) {
        return ApiResponse.ok(recurringItemService.getList(userId, isActive, type));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@AuthenticationPrincipal Long userId,
                                    @PathVariable("id") Long id,
                                    @Valid @RequestBody UpdateRecurringItemRequest request) {
        recurringItemService.update(userId, id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                    @PathVariable("id") Long id) {
        recurringItemService.delete(userId, id);
        return ApiResponse.ok();
    }
}
