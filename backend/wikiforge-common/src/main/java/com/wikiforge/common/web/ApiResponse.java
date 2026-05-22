package com.wikiforge.common.web;

import com.wikiforge.common.error.ErrorCode;

public record ApiResponse<T>(boolean success, T data, String message, String code) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "ok", null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, message, code);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.code(), errorCode.defaultMessage());
    }
}
