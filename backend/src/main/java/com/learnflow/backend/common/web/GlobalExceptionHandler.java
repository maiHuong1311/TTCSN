package com.learnflow.backend.common.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException exception) {
        ProblemDetail detail = problem(
            exception.status(),
            VietnameseMessages.translate(exception.getMessage())
        );
        detail.setTitle(VietnameseMessages.title(exception.status().value()));
        detail.setType(URI.create("about:blank"));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(
            HttpStatus.BAD_REQUEST,
            "Thông tin chưa hợp lệ. Vui lòng kiểm tra lại."
        );
        detail.setTitle("Thông tin không hợp lệ");
        Map<String, String> errors = new LinkedHashMap<>();
        exception
            .getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                errors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage() != null && !error.getDefaultMessage().matches("[\\x00-\\x7F]*")
                        ? error.getDefaultMessage()
                        : VietnameseMessages.validation(error.getCode())
                )
            );
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    })
    ProblemDetail malformed(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên hoặc tham số không hợp lệ.");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ProblemDetail conflict(Exception ex) {
        return problem(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc đang được sử dụng bởi bản ghi khác.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ProblemDetail imageTooLarge(Exception exception) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh không được vượt quá 5 MB.");
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    ProblemDetail missingImage(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh để tải lên.");
    }

    private ProblemDetail problem(HttpStatus status, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(VietnameseMessages.title(status.value()));
        return detail;
    }
}
