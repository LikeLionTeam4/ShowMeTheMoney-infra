package com.showmethemoney.transaction.interfaces;

import com.showmethemoney.common.ApiResponse;
import com.showmethemoney.transaction.application.TransactionListRequest;
import com.showmethemoney.transaction.application.TransactionService;
import com.showmethemoney.transaction.interfaces.dto.CreateTransactionRequest;
import com.showmethemoney.transaction.interfaces.dto.TransactionPageResponse;
import com.showmethemoney.transaction.interfaces.dto.TransactionResponse;
import com.showmethemoney.transaction.interfaces.dto.UpdateTransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody CreateTransactionRequest request) {
        transactionService.create(userId, request);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<TransactionPageResponse> getList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "categoryCode", required = false) String categoryCode,
            @RequestParam(name = "period", required = false) String period,
            @RequestParam(name = "sort", defaultValue = "desc") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(transactionService.getList(userId,
                new TransactionListRequest(type, categoryCode, period, sort, page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getOne(@AuthenticationPrincipal Long userId,
                                                   @PathVariable("id") Long id) {
        return ApiResponse.ok(transactionService.getOne(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@AuthenticationPrincipal Long userId,
                                    @PathVariable("id") Long id,
                                    @Valid @RequestBody UpdateTransactionRequest request) {
        transactionService.update(userId, id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId,
                                    @PathVariable("id") Long id) {
        transactionService.delete(userId, id);
        return ApiResponse.ok();
    }
}
