package com.smartqueue.aws.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class SesNotificationService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.from-name:SmartQueue System}")
    private String fromName;

    public void sendEmail(String toEmail, String subject, String body) {
        try {
            log.info("Sending email to: {} with subject: {}", toEmail, subject);

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(String.format("%s <%s>", fromName, fromEmail))
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .charset("UTF-8")
                                            .data(body)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent successfully to {} with messageId: {}", toEmail, response.messageId());

        } catch (SesException e) {
            log.error("SES Error sending email to {}: {} - {}", toEmail, e.awsErrorDetails().errorCode(),
                    e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to send email via SES", e);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            log.info("Sending HTML email to: {}", toEmail);

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(String.format("%s <%s>", fromName, fromEmail))
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset("UTF-8")
                                            .data(htmlBody)
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("HTML email sent successfully to {} with messageId: {}", toEmail, response.messageId());

        } catch (Exception e) {
            log.error("Failed to send HTML email to {}", toEmail, e);
            throw new RuntimeException("HTML email sending failed", e);
        }
    }
}
