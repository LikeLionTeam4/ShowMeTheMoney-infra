package com.showmethemoney.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YearMonthKeyTest {

    @Test
    void 정상_연월_변환() {
        assertThat(YearMonthKey.toDbFormat("202606")).isEqualTo("2026-06");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdef", "2026-6", "20266", "2026066", "", " "})
    void 숫자_6자리가_아니면_예외(String invalid) {
        assertThatThrownBy(() -> YearMonthKey.toDbFormat(invalid))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void null_이면_예외() {
        assertThatThrownBy(() -> YearMonthKey.toDbFormat(null))
                .isInstanceOf(BusinessException.class);
    }
}
