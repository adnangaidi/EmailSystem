package com.projects.adnan.EmailProject.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException e) {
        return error("error/404", HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge() {
        // compose.html shows a message when ${param.tooLarge} is present
        return new ModelAndView("redirect:/compose?tooLarge");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView generic(Exception e) throws Exception {
        // let Spring Security produce the 403 / redirect to login
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException) {
            throw e;
        }
        // framework exceptions that already carry an HTTP status (404, 405, 400 ...)
        if (e instanceof ErrorResponse er) {
            HttpStatusCode code = er.getStatusCode();
            String view = code.value() == 404 ? "error/404" : "error/500";
            return error(view, HttpStatus.valueOf(code.value()), "The request could not be processed.");
        }
        log.error("Unexpected error", e);
        return error("error/500", HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private ModelAndView error(String view, HttpStatus status, String message) {
        ModelAndView mv = new ModelAndView(view);
        mv.setStatus(status);
        mv.addObject("message", message);
        return mv;
    }
}
