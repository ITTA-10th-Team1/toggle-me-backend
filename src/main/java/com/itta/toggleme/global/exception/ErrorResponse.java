package com.itta.toggleme.global.exception;

import java.util.List;
import org.springframework.validation.BindingResult;

public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> errors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        List<FieldErrorDetail> errors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }

    public record FieldErrorDetail(
            String field,
            String reason
    ) {
    }
}
