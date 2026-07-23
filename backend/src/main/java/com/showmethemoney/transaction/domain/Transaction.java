package com.showmethemoney.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaction {

    private Long uuid;
    private Long uuidUser;
    private Long uuidCategory;
    private Long uuidRecurringItem;
    private String categoryCode;
    private String categoryName;
    private Integer type; // 0=EXPENSE, 1=INCOME
    private BigDecimal amount;
    private String memo;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Long getUuid() { return uuid; }
    public Long getUuidUser() { return uuidUser; }
    public Long getUuidCategory() { return uuidCategory; }
    public Long getUuidRecurringItem() { return uuidRecurringItem; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public Integer getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getMemo() { return memo; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setUuid(Long uuid) { this.uuid = uuid; }
    public void setUuidUser(Long uuidUser) { this.uuidUser = uuidUser; }
    public void setUuidCategory(Long uuidCategory) { this.uuidCategory = uuidCategory; }
    public void setUuidRecurringItem(Long uuidRecurringItem) { this.uuidRecurringItem = uuidRecurringItem; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setType(Integer type) { this.type = type; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setMemo(String memo) { this.memo = memo; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
