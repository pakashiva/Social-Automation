package com.shoppingmall.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "error/404";
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden() {
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)
    public Object business(BusinessException exception, RedirectAttributes redirectAttributes) {
        if (exception.getHttpStatus() == 400) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(exception.getMessage());
        }
        if (exception.getRedirectPath() != null) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:" + exception.getRedirectPath();
        }
        log.error("business exception", exception);
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception exception) {
        log.error("request failed", exception);
        return "error/500";
    }
}
