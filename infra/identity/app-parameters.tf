variable "app_db_password" {
  description = "Password for the application database connection"
  type        = string
  sensitive   = true
  ephemeral   = true
}

variable "app_jwt_secret" {
  description = "Base64-encoded JWT signing key"
  type        = string
  sensitive   = true
  ephemeral   = true
}

resource "aws_ssm_parameter" "app_db_password" {
  name             = "/ums/app/db-password"
  description      = "Database password for the UMS application"
  type             = "SecureString"
  tier             = "Standard"
  value_wo         = var.app_db_password
  value_wo_version = 1
}

resource "aws_ssm_parameter" "app_jwt_secret" {
  name             = "/ums/app/jwt-secret"
  description      = "JWT signing key for the UMS application"
  type             = "SecureString"
  tier             = "Standard"
  value_wo         = var.app_jwt_secret
  value_wo_version = 1
}
