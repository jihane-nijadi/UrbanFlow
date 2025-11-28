data "aws_caller_identity" "current" {}

output "aws_account_id" {
  description = "AWS account id where repositories are created"
  value       = data.aws_caller_identity.current.account_id
}

output "ecr_repository_urls" {
  description = "Map of ECR repository names to URLs"
  value = {
    for name, repo in aws_ecr_repository.urbanflow :
    name => repo.repository_url
  }
}


