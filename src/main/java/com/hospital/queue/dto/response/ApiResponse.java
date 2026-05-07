package com.hospital.queue.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String  message;
    private T       data;
    private Object  errors;
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data) {
        var r = new ApiResponse<T>();
        r.success = true;
        r.data    = data;
        return r;
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        var r = ok(data);
        r.message = message;
        return r;
    }

    public static <T> ApiResponse<T> error(String message) {
        var r = new ApiResponse<T>();
        r.success = false;
        r.message = message;
        return r;
    }

    public static <T> ApiResponse<T> validationError(String message, Object errors) {
        var r = ApiResponse.<T>error(message);
        r.errors = errors;
        return r;
    }
}
