package com.hospital.queue.service;

import com.hospital.queue.domain.entity.Patient;
import com.hospital.queue.domain.entity.QueueEntry;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender  mailSender;
    private final TemplateEngine  templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Async
    public void sendQueueConfirmation(Patient patient, QueueEntry entry, int position) {
        if (patient.getEmail() == null || patient.getEmail().isBlank()) return;
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("patientName",   patient.getFullName());
            ctx.setVariable("ticketNumber",  entry.getTicketNumber());
            ctx.setVariable("department",    entry.getDepartment().getName());
            ctx.setVariable("position",      position);
            ctx.setVariable("priority",      entry.getPriority().name());
            ctx.setVariable("estimatedWait", entry.getEstimatedWaitMin());
            ctx.setVariable("chiefComplaint",entry.getChiefComplaint());
            ctx.setVariable("registeredAt",  entry.getRegisteredAt());

            String html = templateEngine.process("email/queue-confirmation", ctx);
            sendHtmlEmail(patient.getEmail(), "Queue Confirmation — " + entry.getTicketNumber(), html);
        } catch (Exception e) {
            log.error("Failed to send queue confirmation to {}: {}", patient.getEmail(), e.getMessage());
        }
    }

    @Async
    public void sendReportEmail(String toEmail, String toName, byte[] reportBytes,
                                String filename, String reportType) {
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("recipientName", toName);
            ctx.setVariable("reportType",    reportType);
            ctx.setVariable("filename",      filename);

            String html = templateEngine.process("email/report-delivery", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Hospital Queue Report — " + reportType);
            helper.setText(html, true);
            helper.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(reportBytes));

            mailSender.send(msg);
            log.info("Report email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send report email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendAppointmentReminder(Patient patient, QueueEntry entry) {
        if (patient.getEmail() == null || patient.getEmail().isBlank()) return;
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("patientName", patient.getFullName());
            ctx.setVariable("ticketNumber", entry.getTicketNumber());
            ctx.setVariable("department",   entry.getDepartment().getName());
            ctx.setVariable("doctorName",   entry.getDoctor() != null
                    ? entry.getDoctor().getUser().getFullName() : "A doctor");

            String html = templateEngine.process("email/appointment-reminder", ctx);
            sendHtmlEmail(patient.getEmail(), "It's Almost Your Turn — " + entry.getTicketNumber(), html);
        } catch (Exception e) {
            log.error("Failed to send appointment reminder: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(msg);
        log.info("Email sent: [{}] → {}", subject, to);
    }
}
