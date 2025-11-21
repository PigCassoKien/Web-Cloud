package com.smartqueue.aws.event;

import com.smartqueue.aws.event.UserRegisteredEvent;
import com.smartqueue.aws.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationListener {

    private final SesClient sesClient;

    @Async  // gửi email không block API
    @EventListener
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        User user = event.getUser();

        // Chỉ gửi nếu người dùng bật thông báo email
        if (!user.isEmailNotificationEnabled()) {
            log.info("Email notification disabled for user: {}", user.getEmail());
            return;
        }

        String subject = "Chào mừng bạn đến với SmartQueue!";
        String htmlBody = """
            <h2>Xin chào %s!</h2>
            <p>Cảm ơn bạn đã đăng ký sử dụng <strong>SmartQueue</strong> – Hệ thống xếp hàng thông minh.</p>
            <p>Thông tin tài khoản của bạn:</p>
            <ul>
                <li>Email: <strong>%s</strong></li>
                <li>Số điện thoại: <strong>%s</strong></li>
            </ul>
            <p>Bạn sẽ nhận được thông báo qua email khi gần đến lượt phục vụ.</p>
            <br>
            <p>Trân trọng,<br><strong>Đội ngũ SmartQueue</strong></p>
            """.formatted(user.getName(), user.getEmail(), user.getPhone() != null ? user.getPhone() : "Chưa cung cấp");

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source("SmartQueue System <kien0610minh@gmail.com>")
                    .destination(Destination.builder().toAddresses(user.getEmail()).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);
            log.info("Welcome email sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }
}