package com.hospital.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class HospitalQueueApplication {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(HospitalQueueApplication.class, args);
        String port = ctx.getEnvironment().getProperty("server.port", "8080");
        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║     🏥  Hospital Queue Management System  STARTED        ║");
        log.info("║     URL: http://localhost:{}                            ║", port);
        log.info("║     H2 Console: http://localhost:{}/h2-console         ║", port);
        log.info("║     Default credentials: admin / Password@123           ║");
        log.info("╚══════════════════════════════════════════════════════════╝");
    }
}
