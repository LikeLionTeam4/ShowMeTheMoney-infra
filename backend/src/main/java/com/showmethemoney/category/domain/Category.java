package com.showmethemoney.category.domain;

public class Category {

    private Long uuid;
    private String code;
    private String codeNumber;
    private String name;
    private Integer type; // 0=EXPENSE, 1=INCOME

    public Long getUuid() { return uuid; }
    public String getCode() { return code; }
    public String getCodeNumber() { return codeNumber; }
    public String getName() { return name; }
    public Integer getType() { return type; }

    public void setUuid(Long uuid) { this.uuid = uuid; }
    public void setCode(String code) { this.code = code; }
    public void setCodeNumber(String codeNumber) { this.codeNumber = codeNumber; }
    public void setName(String name) { this.name = name; }
    public void setType(Integer type) { this.type = type; }
}
