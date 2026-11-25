resource "aws_vpc" "ums" {
  cidr_block           = "10.20.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name    = "ums-vpc"
    Project = "university-management-system"
  }
}

resource "aws_subnet" "private_db_a" {
  vpc_id                  = aws_vpc.ums.id
  cidr_block              = "10.20.10.0/24"
  availability_zone       = "ap-southeast-1a"
  map_public_ip_on_launch = false

  tags = {
    Name    = "ums-private-db-a"
    Project = "university-management-system"
  }
}

resource "aws_subnet" "private_db_b" {
  vpc_id                  = aws_vpc.ums.id
  cidr_block              = "10.20.20.0/24"
  availability_zone       = "ap-southeast-1b"
  map_public_ip_on_launch = false

  tags = {
    Name    = "ums-private-db-b"
    Project = "university-management-system"
  }
}
