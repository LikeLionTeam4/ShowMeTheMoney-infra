package com.showmethemoney.dashboard.infrastructure;

import java.math.BigDecimal;

public class CategoryAmountRow {

    private String categoryCode;
    private String categoryName;
    private BigDecimal amount;

    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public BigDecimal getAmount() { return amount; }
}
