resource "aws_instance" "app" {
  ami                         = "ami-026b70e9abd9fa20f"
  instance_type               = "t3.small"
  subnet_id                   = aws_subnet.app.id
  vpc_security_group_ids      = [aws_security_group.app.id]
  associate_public_ip_address = true
  iam_instance_profile        = "ums-app-ec2"

  user_data = replace(templatefile("${path.module}/app-user-data.sh", {
    aws_region = "ap-southeast-1"
    db_host    = aws_db_instance.ums.address
  }), "\r\n", "\n")

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 20
    encrypted             = true
    delete_on_termination = true
  }

  metadata_options {
    http_tokens = "required"
  }

  credit_specification {
    cpu_credits = "standard"
  }

  tags = {
    Name    = "ums-app"
    Project = "university-management-system"
  }
}
