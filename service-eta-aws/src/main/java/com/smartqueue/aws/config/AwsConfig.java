package com.smartqueue.aws.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;

import java.net.URI;

@Configuration
@Slf4j
public class AwsConfig {

    @Value("${aws.region:ap-southeast-1}")
    private String region;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamodbEndpoint;

    @Value("${aws.ses.endpoint:}")
    private String sesEndpoint;

    @Value("${aws.sns.endpoint:}")
    private String snsEndpoint;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // === CREDENTIALS PROVIDER ===
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        log.info("Using DefaultCredentialsProvider (IAM Role/ENV/Config file)");
        return DefaultCredentialsProvider.create();
    }

    // === DYNAMODB CLIENT ===
    @Bean
    @ConditionalOnProperty(name = "aws.dynamodb.enabled", havingValue = "true", matchIfMissing = true)
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider credentialsProvider) {
        try {
            log.info("Initializing DynamoDB client in region: {}", region);

            DynamoDbClientBuilder builder = DynamoDbClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .httpClient(UrlConnectionHttpClient.builder().build());

            if (dynamodbEndpoint != null && !dynamodbEndpoint.isBlank()) {
                log.info("Using local DynamoDB endpoint: {}", dynamodbEndpoint);
                builder.endpointOverride(URI.create(dynamodbEndpoint));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to create DynamoDB client", e);
            throw new RuntimeException("DynamoDB initialization failed", e);
        }
    }

    // === SES CLIENT ===
    @Bean
    @ConditionalOnProperty(name = "aws.ses.enabled", havingValue = "true", matchIfMissing = true)
    public SesClient sesClient(AwsCredentialsProvider credentialsProvider) {
        try {
            log.info("Initializing SES client in region: {}", region);

            SesClientBuilder builder = SesClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .httpClient(UrlConnectionHttpClient.builder().build());

            if (sesEndpoint != null && !sesEndpoint.isBlank()) {
                log.info("Using local SES endpoint: {}", sesEndpoint);
                builder.endpointOverride(URI.create(sesEndpoint));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to create SES client", e);
            throw new RuntimeException("SES initialization failed", e);
        }
    }

    // === SNS CLIENT ===
    @Bean
    @ConditionalOnProperty(name = "aws.sns.enabled", havingValue = "true", matchIfMissing = true)
    public SnsClient snsClient(AwsCredentialsProvider credentialsProvider) {
        try {
            log.info("Initializing SNS client in region: {}", region);

            SnsClientBuilder builder = SnsClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .httpClient(UrlConnectionHttpClient.builder().build());

            if (snsEndpoint != null && !snsEndpoint.isBlank()) {
                log.info("Using local SNS endpoint: {}", snsEndpoint);
                builder.endpointOverride(URI.create(snsEndpoint));
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to create SNS client", e);
            throw new RuntimeException("SNS initialization failed", e);
        }
    }

    @Bean
    public String dynamodbTablePrefix() {
        return "smartqueue-" + activeProfile + "-";
    }

    @Bean
    public String awsRegion() {
        return region;
    }
}
