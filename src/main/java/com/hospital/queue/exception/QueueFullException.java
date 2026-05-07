package com.hospital.queue.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class QueueFullException extends RuntimeException {
    public QueueFullException(String department) {
        super("Queue is full for department: " + department + ". No more patients can be registered.");
    }
}
