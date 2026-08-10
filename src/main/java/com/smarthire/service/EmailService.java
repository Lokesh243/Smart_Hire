package com.smarthire.service;

import com.smarthire.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // ── Application received ───────────────────────────────────────
    @Async("emailExecutor")
    public void sendApplicationConfirmation(String toEmail,
                                            String candidateName,
                                            String jobTitle) {
        String subject = "Application Received — " + jobTitle;
        String body = String.format(
                "Hi %s,\n\nYour application for '%s' has been received.\n" +
                        "We will review your profile and get back to you soon.\n\n" +
                        "Best regards,\nSmartHire Team",
                candidateName, jobTitle
        );
        sendEmail(toEmail, subject, body);
    }

    // ── Stage changed ──────────────────────────────────────────────
    @Async("emailExecutor")
    public void sendStageUpdateEmail(String toEmail,
                                     String candidateName,
                                     String jobTitle,
                                     ApplicationStatus newStatus) {
        String subject = "Application Update — " + jobTitle;
        String body = String.format(
                "Hi %s,\n\nYour application for '%s' has been updated.\n\n" +
                        "Current Status: %s\n%s\n\nBest regards,\nSmartHire Team",
                candidateName, jobTitle,
                newStatus.name(),
                getStatusMessage(newStatus)
        );
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("gajangilokesh@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String getStatusMessage(ApplicationStatus status) {
        return switch (status) {
            case SCREENED  -> "Your profile has been shortlisted for further review.";
            case INTERVIEW -> "You have been selected for an interview. HR will contact you shortly.";
            case OFFER     -> "We are pleased to extend an offer to you. Congratulations!";
            case REJECTED  -> "Thank you for your interest. We will not be moving forward at this time.";
            default        -> "Your application status has been updated.";
        };
    }
}