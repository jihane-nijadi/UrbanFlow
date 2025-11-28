locals {
  ecr_repo_prefix = var.project_name
}

resource "aws_ecr_repository" "urbanflow" {
  for_each = toset(var.ecr_repositories)

  name = each.value

  image_scanning_configuration {
    scan_on_push = true
  }

  image_tag_mutability = "MUTABLE"

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Project = var.project_name
  }
}


