package com.orderhub.productservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
