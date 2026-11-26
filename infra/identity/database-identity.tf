terraform {
  required_version = ">= 1.15.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 6.23.0, < 7.0.0"
    }
  }
}

provider "aws" {
  region              = "ap-southeast-1"
  allowed_account_ids = ["299482582327"]
}

locals {
  rds_arn_prefix = "arn:aws:rds:ap-southeast-1:299482582327"
  database_arn   = "${local.rds_arn_prefix}:db:ums-postgres"
  subnet_arn     = "${local.rds_arn_prefix}:subgrp:ums-db-subnets"
}

resource "aws_iam_role" "database" {
  name        = "ums-terraform-database"
  description = "Terraform access to the UMS PostgreSQL instance"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { AWS = "arn:aws:iam::299482582327:user/ums-operator" }
    }]
  })
}

resource "aws_iam_role_policy" "database" {
  name = "ums-rds-instance"
  role = aws_iam_role.database.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "CreateApprovedInstance"
        Effect   = "Allow"
        Action   = "rds:CreateDBInstance"
        Resource = local.database_arn
        Condition = {
          StringEquals = {
            "rds:DatabaseEngine" = "postgres"
            "rds:DatabaseClass"  = "db.t4g.micro"
          }
        }
      },
      {
        Sid    = "UseExistingRDSGroupsForCreation"
        Effect = "Allow"
        Action = "rds:CreateDBInstance"
        Resource = [
          local.subnet_arn,
          "${local.rds_arn_prefix}:pg:default.postgres18",
          "${local.rds_arn_prefix}:og:default:postgres-18",
        ]
      },
      {
        Sid    = "ManageApprovedInstance"
        Effect = "Allow"
        Action = [
          "rds:ListTagsForResource",
          "rds:AddTagsToResource",
          "rds:DeleteDBInstance",
          "rds:ModifyDBInstance",
        ]
        Resource = local.database_arn
      },
      {
        Sid      = "DescribeRegionalDBInstances"
        Effect   = "Allow"
        Action   = "rds:DescribeDBInstances"
        Resource = "${local.rds_arn_prefix}:db:*"
      },
      {
        Sid      = "DescribeExistingSubnetGroup"
        Effect   = "Allow"
        Action   = "rds:DescribeDBSubnetGroups"
        Resource = local.subnet_arn
      },
    ]
  })
}
