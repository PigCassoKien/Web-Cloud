package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Value("${aws.dynamodb.users-table}")
    private String tableName;

    private DynamoDbTable<User> getUserTable() {
        return dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(User.class));
    }

    public User save(User user) {
        log.debug("Saving user: {}", user.getUserId());
        getUserTable().putItem(user);
        return user;
    }

    public Optional<User> findById(String userId) {
        log.debug("Finding user by ID: {}", userId);
        Key key = Key.builder().partitionValue(userId).build();
        User user = getUserTable().getItem(key);
        return Optional.ofNullable(user);
    }

    public void delete(String userId) {
        log.debug("Deleting user: {}", userId);
        Key key = Key.builder().partitionValue(userId).build();
        getUserTable().deleteItem(key);
    }

    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);

        try {
            Expression filterExpression = Expression.builder()
                    .expression("email = :email")
                    .putExpressionValue(":email", AttributeValue.builder().s(email).build())
                    .build();

            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(filterExpression)
                    .build();

            return getUserTable().scan(scanRequest)
                    .items()
                    .stream()
                    .findFirst();
        } catch (Exception e) {
            log.error("Error finding user by email: {}", email, e);
            return Optional.empty();
        }
    }
}
