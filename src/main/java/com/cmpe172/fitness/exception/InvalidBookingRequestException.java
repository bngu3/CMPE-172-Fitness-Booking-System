package com.cmpe172.fitness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidBookingRequestException extends RuntimeException {
    public InvalidBookingRequestException() {
        super("A valid session must be selected.");
    }
}
