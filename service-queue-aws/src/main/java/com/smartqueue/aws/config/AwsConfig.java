package com.smartqueue.aws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    @Value("${aws.dynamodb.table-prefix}")
    private String tablePrefix;

    @Value("${aws.dynamodb.tickets-table}")
    private String ticketsTableName;

    @Value("${aws.dynamodb.queues-table}")
    private String queuesTableName;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (dynamoDbEndpoint != null
                && !dynamoDbEndpoint.trim().isEmpty()
                && !dynamoDbEndpoint.trim().startsWith("#")) {
            builder.endpointOverride(URI.create(dynamoDbEndpoint.trim()));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public String ticketsTableName() {
        return tablePrefix + ticketsTableName;
    }

    @Bean
    public String queuesTableName() {
        return tablePrefix + queuesTableName;
    }

    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }
}