package com.cmpe172.fitness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAvailabilityRequestException extends RuntimeException {
    public InvalidAvailabilityRequestException() {
        super("Provide a service and a future time range with an end after the start.");
    }
}
