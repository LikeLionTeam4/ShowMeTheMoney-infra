package com.showmethemoney.dashboard.infrastructure;

import java.math.BigDecimal;

public class DailyAmountRow {

    private String txDate;
    private int type;
    private BigDecimal amount;

    public String getTxDate() { return txDate; }
    public int getType() { return type; }
    public BigDecimal getAmount() { return amount; }
}
