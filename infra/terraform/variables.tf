variable "aws_region" {
  description = "AWS region to deploy infrastructure resources into"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used as prefix for resource naming"
  type        = string
  default     = "urbanflow"
}

variable "ecr_repositories" {
  description = "List of ECR repositories to create/manage for UrbanFlow services"
  type        = list(string)
  default = [
    "urbanflow-api-gateway",
    "urbanflow-user-service",
    "urbanflow-payment-service",
    "urbanflow-ticket-service",
    "urbanflow-route-service",
    "urbanflow-geolocation-service",
    "urbanflow-frontend",
  ]
}

variable "vpc_cidr" {
  description = "CIDR block for the UrbanFlow VPC"
  type        = string
  default     = "10.50.0.0/16"
}

variable "public_subnets" {
  description = "Public subnet CIDR blocks"
  type        = list(string)
  default     = ["10.50.0.0/24", "10.50.1.0/24"]
}

variable "private_subnets" {
  description = "Private subnet CIDR blocks"
  type        = list(string)
  default     = ["10.50.10.0/24", "10.50.11.0/24"]
}

variable "availability_zones" {
  description = "Availability zones to use for subnets"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}


