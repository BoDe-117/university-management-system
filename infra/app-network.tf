resource "aws_subnet" "app" {
  vpc_id                  = aws_vpc.ums.id
  cidr_block              = "10.20.1.0/24"
  availability_zone       = "ap-southeast-1a"
  map_public_ip_on_launch = false

  tags = {
    Name    = "ums-app-subnet"
    Project = "university-management-system"
  }
}

resource "aws_internet_gateway" "ums" {
  vpc_id = aws_vpc.ums.id

  tags = {
    Name    = "ums-internet-gateway"
    Project = "university-management-system"
  }
}

resource "aws_route_table" "app" {
  vpc_id = aws_vpc.ums.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.ums.id
  }

  tags = {
    Name    = "ums-app-route-table"
    Project = "university-management-system"
  }
}

resource "aws_route_table_association" "app" {
  subnet_id      = aws_subnet.app.id
  route_table_id = aws_route_table.app.id
}
