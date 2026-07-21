package com.showmethemoney.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // Auth
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "거래 유형과 카테고리 유형이 일치하지 않습니다."),

    // Transaction
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 내역을 찾을 수 없습니다."),
    INVALID_PERIOD_FORMAT(HttpStatus.BAD_REQUEST, "period 형식이 올바르지 않습니다. (YYYY / YYYY-MM / YYYY-MM-DD)"),

    // RecurringItem
    RECURRING_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "고정 항목을 찾을 수 없습니다."),

    // Budget
    BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "예산을 찾을 수 없습니다."),
    BUDGET_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 월의 예산이 이미 존재합니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
