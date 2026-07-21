package com.showmethemoney.common;

public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode code) {
        return new ApiResponse<>(false, null, new ErrorDetail(code.name(), code.getMessage()));
    }
}
