package com.roadbook.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        return r;
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = errorCode.getCode();
        r.message = message;
        return r;
    }
}
