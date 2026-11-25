resource "aws_db_subnet_group" "ums" {
  name = "ums-db-subnets"

  subnet_ids = [
    aws_subnet.private_db_a.id,
    aws_subnet.private_db_b.id,
  ]

  tags = {
    Name    = "ums-db-subnets"
    Project = "university-management-system"
  }
}
