package com.showmethemoney.common;

import java.util.regex.Pattern;

public final class YearMonthKey {

    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("^\\d{6}$");

    private YearMonthKey() {}

    // "202606" → "2026-06"
    public static String toDbFormat(String yearMonth) {
        if (yearMonth == null || !YEAR_MONTH_PATTERN.matcher(yearMonth).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return yearMonth.substring(0, 4) + "-" + yearMonth.substring(4);
    }
}
