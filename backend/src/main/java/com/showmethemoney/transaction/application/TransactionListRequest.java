package com.showmethemoney.transaction.application;

public record TransactionListRequest(
        Integer type,
        String categoryCode,
        String period,
        String sort,
        int page,
        int size
) {
    public TransactionListRequest {
        if (sort == null) sort = "desc";
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
    }
}
