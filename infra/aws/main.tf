terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "smartqueue"
}

# DynamoDB Tables
resource "aws_dynamodb_table" "tickets" {
  name           = "${var.project_name}-tickets-${var.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "ticketId"

  attribute {
    name = "ticketId"
    type = "S"
  }

  attribute {
    name = "queueId"
    type = "S"
  }

  global_secondary_index {
    name            = "queueId-index"
    hash_key        = "queueId"
    projection_type = "ALL"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_dynamodb_table" "queues" {
  name           = "${var.project_name}-queues-${var.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "queueId"

  attribute {
    name = "queueId"
    type = "S"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# S3 Bucket for Frontend
resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${var.environment}-${random_string.bucket_suffix.result}"

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "index.html"
  }
}

# CloudFront Distribution
resource "aws_cloudfront_distribution" "frontend" {
  origin {
    domain_name = aws_s3_bucket_website_configuration.frontend.website_endpoint
    origin_id   = "S3-${aws_s3_bucket.frontend.id}"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"

  default_cache_behavior {
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${aws_s3_bucket.frontend.id}"
    compress               = true
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# API Gateway for Lambda
resource "aws_api_gateway_rest_api" "queue_api" {
  name        = "${var.project_name}-queue-api-${var.environment}"
  description = "SmartQueue API Gateway"

  endpoint_configuration {
    types = ["REGIONAL"]
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Outputs
output "dynamodb_tickets_table_name" {
  value = aws_dynamodb_table.tickets.name
}

output "dynamodb_queues_table_name" {
  value = aws_dynamodb_table.queues.name
}

output "s3_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "cloudfront_domain" {
  value = aws_cloudfront_distribution.frontend.domain_name
}

output "api_gateway_url" {
  value = aws_api_gateway_rest_api.queue_api.execution_arn
}