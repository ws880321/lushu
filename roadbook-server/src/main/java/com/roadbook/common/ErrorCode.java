package com.roadbook.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    TEMPLATE_NOT_FOUND(40401, "未找到匹配的路线模板"),
    INTERNAL_ERROR(50000, "服务器内部错误"),
    AMAP_API_ERROR(50001, "地图服务调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
