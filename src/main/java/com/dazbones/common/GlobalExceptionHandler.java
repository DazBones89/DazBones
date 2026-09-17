package com.dazbones.common;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleStatus(ResponseStatusException e, HttpServletRequest request, HttpServletResponse response) {
        return error(e.getStatusCode().value(), request, response);
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            java.time.format.DateTimeParseException.class})
    public Object handleBadInput(Exception e, HttpServletRequest request, HttpServletResponse response) {
        return error(400, request, response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleValidation(IllegalArgumentException e,HttpServletRequest request,HttpServletResponse response){
        return error(400,request,response);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public Object handleUpload(Exception e,HttpServletRequest request,HttpServletResponse response){
        response.setStatus(413);return new ModelAndView("error/413");
    }

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class, org.springframework.dao.DataIntegrityViolationException.class})
    public Object handleConflict(Exception e, HttpServletRequest request, HttpServletResponse response) {
        return error(409, request, response);
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.error("Request failed: {} {}", request.getMethod(), request.getRequestURI(), e);
        return error(500, request, response);
    }

    private Object error(int status, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(status);
        if (request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/survey/")) {
            return ResponseEntity.status(status).body(java.util.Map.of("success", false, "message",
                    status == 400 ? "入力内容を確認してください" : status == 409 ? "他の画面で更新されました。再読み込みして確認してください" : "処理に失敗しました"));
        }
        return new ModelAndView("error/" + (status == 400 ? "400" : status == 404 ? "404" : status == 403 ? "403" : status == 409 ? "409" : "500"));
    }
}
