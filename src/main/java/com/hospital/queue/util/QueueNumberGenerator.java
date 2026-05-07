package com.hospital.queue.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class QueueNumberGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile String currentDate = LocalDate.now().format(DATE_FMT);

    public synchronized String nextTicket(String departmentCode) {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(currentDate)) {
            currentDate = today;
            counter.set(0);
        }
        int seq = counter.incrementAndGet();
        return String.format("TKT-%s-%03d", departmentCode.toUpperCase(), seq);
    }

    public synchronized String nextPatientNumber() {
        return String.format("PAT-%05d", System.currentTimeMillis() % 100000);
    }
}
