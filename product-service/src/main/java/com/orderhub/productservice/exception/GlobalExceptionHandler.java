package com.orderhub.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

// @RestControllerAdvice: tüm controller'lardan fırlayan exception'ları tek yerde yakalar (AOP mantığı)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bu exception tipi fırlarsa Spring bu metodu çağırır
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // alan → mesaj. LinkedHashMap: alanlar eklendiği sırada kalsın
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                // Bir alan birden fazla kurala takılabilir (sku: @NotBlank + @Pattern); ilkini tut
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        // ProblemDetail: Spring'in RFC 9457 hata modeli (type, title, status, detail, instance)
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "İstek doğrulamadan geçemedi");

        problem.setTitle("Validation Failed");
        // Standart dışı alanlar setProperty ile eklenir → JSON'da üst seviyede görünür
        problem.setProperty("errors", errors);

        return problem;
    }

    // Parametre kısıtı (@Min/@Max...) ihlal edilirse bu exception fırlar
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        // Her parametrenin sonucu → parametre adı + mesaj
        ex.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error ->
                        errors.putIfAbsent(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "İstek parametreleri doğrulamadan geçemedi");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    // Service'ten fırlayan "kaynak yok" hatası → 404
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        // detail: exception mesajı ("Product not found: <id>")
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage()
        );
        problem.setTitle("Product Not Found");
        return problem;
    }

}
