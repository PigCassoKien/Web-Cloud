package com.smartqueue.aws.dto.user;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private String userId;
    private String name;
    private String email;
    private String phone;
    private boolean emailNotificationEnabled;
    private boolean smsNotificationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean isActive;
}