variable "db_master_password" {
  description = "Master password for the UMS PostgreSQL instance"
  type        = string
  sensitive   = true
  ephemeral   = true
}

resource "aws_db_instance" "ums" {
  provider = aws.database

  identifier               = "ums-postgres"
  engine                   = "postgres"
  engine_version           = "18.3"
  engine_lifecycle_support = "open-source-rds-extended-support-disabled"
  instance_class           = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name             = "umsdb"
  username            = "umsadmin"
  password_wo         = var.db_master_password
  password_wo_version = 1

  db_subnet_group_name   = aws_db_subnet_group.ums.name
  vpc_security_group_ids = [aws_security_group.db.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period    = 1
  auto_minor_version_upgrade = true
  deletion_protection        = true

  tags = {
    Name    = "ums-postgres"
    Project = "university-management-system"
  }
}
