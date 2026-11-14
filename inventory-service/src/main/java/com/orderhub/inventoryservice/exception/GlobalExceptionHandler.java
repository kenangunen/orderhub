package com.orderhub.inventoryservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

// Tüm controller'lardan fırlayan exception'ları tek yerde yakalar
// Not: Request body alan endpoint yok → MethodArgumentNotValidException handler'ı gerekmiyor
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Parametre kısıtı ihlali (ör. sku @Pattern) → 400
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        // Her parametrenin sonucu → parametre adı + ilk mesaj
        ex.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error ->
                        errors.putIfAbsent(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "İstek parametreleri doğrulamadan geçemedi");
        problem.setTitle("Validation Failed");                       // ← "failed" → "Failed"
        problem.setProperty("errors", errors);
        return problem;
    }

    // Service'ten fırlayan "kayıt yok" hatası → 404
    @ExceptionHandler(InventoryItemNotFoundException.class)
    public ProblemDetail handleInventoryItemNotFound(InventoryItemNotFoundException ex) {   // ← metot adı
        // detail: exception mesajı ("Inventory item not found: <sku>")             // ← <id> → <sku>
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Inventory Item Not Found");                 // ← Title Case
        return problem;
    }
}