package com.smartqueue.aws.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class User {
    private String userId;
    private String email;
    private String phone;
    private String name;
    private String password;
    private boolean emailNotificationEnabled;
    private boolean smsNotificationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean isActive;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() { 
        return userId; 
    }

    @DynamoDbAttribute("email")
    public String getEmail() { 
        return email; 
    }

    @DynamoDbAttribute("phone")
    public String getPhone() { 
        return phone; 
    }

    @DynamoDbAttribute("name")
    public String getName() { 
        return name; 
    }

    @DynamoDbAttribute("password")
    public String getPassword() { 
        return password; 
    }

    @DynamoDbAttribute("emailNotificationEnabled")
    public boolean isEmailNotificationEnabled() { 
        return emailNotificationEnabled; 
    }

    @DynamoDbAttribute("smsNotificationEnabled")
    public boolean isSmsNotificationEnabled() { 
        return smsNotificationEnabled; 
    }

    @DynamoDbAttribute("createdAt")
    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }

    @DynamoDbAttribute("lastLoginAt")
    public LocalDateTime getLastLoginAt() { 
        return lastLoginAt; 
    }

    @DynamoDbAttribute("isActive")
    public boolean isActive() { 
        return isActive; 
    }
}