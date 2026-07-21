package com.showmethemoney.recurringitem.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RecurringItem {

    private Long uuid;
    private Long uuidUser;
    private Long uuidCategory;
    private String categoryCode;
    private String categoryName;
    private Integer type; // 0=EXPENSE, 1=INCOME
    private String name;
    private BigDecimal amount;
    private Integer billingDay;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public Long getUuid() { return uuid; }
    public Long getUuidUser() { return uuidUser; }
    public Long getUuidCategory() { return uuidCategory; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public Integer getType() { return type; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public Integer getBillingDay() { return billingDay; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setUuid(Long uuid) { this.uuid = uuid; }
    public void setUuidUser(Long uuidUser) { this.uuidUser = uuidUser; }
    public void setUuidCategory(Long uuidCategory) { this.uuidCategory = uuidCategory; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setType(Integer type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setBillingDay(Integer billingDay) { this.billingDay = billingDay; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
