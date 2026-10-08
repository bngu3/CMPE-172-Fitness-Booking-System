package com.cmpe172.fitness.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.converter.HttpMessageNotReadableException;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({InvalidBookingRequestException.class, InvalidAvailabilityRequestException.class,
            ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class, BindException.class})
    public Object handleBadRequest(Exception exception, HttpServletRequest request) {
        String detail = exception instanceof InvalidBookingRequestException
                || exception instanceof InvalidAvailabilityRequestException
                ? exception.getMessage()
                : "One or more request values are invalid.";
        return respond(HttpStatus.BAD_REQUEST, "Invalid request", detail, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Not found", exception.getMessage(), request);
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public Object handleConflict(SlotUnavailableException exception, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Request conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "Request conflict",
                "The requested change conflicts with existing booking data.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleForbidden(AccessDeniedException exception, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "Forbidden", "You do not have access to this resource.", request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception exception, HttpServletRequest request) {
        logger.error("Unhandled error while processing {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "The request could not be completed. Please try again later.", request);
    }

    private Object respond(HttpStatus status, String title, String detail, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        boolean htmlRequest = accept != null && accept.contains(MediaType.TEXT_HTML_VALUE)
                && !request.getRequestURI().startsWith("/api/");
        if (htmlRequest) {
            ModelAndView view = new ModelAndView("error");
            view.setStatus(status);
            view.addObject("status", status.value());
            view.addObject("title", title);
            view.addObject("detail", detail);
            return view;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
