resource "aws_security_group" "app" {
  name        = "ums-app-sg"
  description = "Security group for the UMS application server"
  vpc_id      = aws_vpc.ums.id

  tags = {
    Name    = "ums-app-sg"
    Project = "university-management-system"
  }
}

resource "aws_security_group" "db" {
  name        = "ums-db-sg"
  description = "Security group for the UMS PostgreSQL database"
  vpc_id      = aws_vpc.ums.id

  tags = {
    Name    = "ums-db-sg"
    Project = "university-management-system"
  }
}

resource "aws_vpc_security_group_ingress_rule" "db_from_app" {
  security_group_id            = aws_security_group.db.id
  referenced_security_group_id = aws_security_group.app.id
  description                  = "Allow PostgreSQL connections from the application"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

resource "aws_vpc_security_group_egress_rule" "app_to_db" {
  security_group_id            = aws_security_group.app.id
  referenced_security_group_id = aws_security_group.db.id
  description                  = "Allow PostgreSQL connections to the database"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
}

resource "aws_vpc_security_group_egress_rule" "app_https" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  description       = "Allow outbound HTTPS connections"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}
