resource "aws_iam_role" "app" {
  name        = "ums-app-ec2"
  description = "Allow the UMS EC2 instance to use Session Manager"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "app_session_manager" {
  name = "ums-app-session-manager"
  role = aws_iam_role.app.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "ssm:UpdateInstanceInformation",
        "ssmmessages:CreateControlChannel",
        "ssmmessages:CreateDataChannel",
        "ssmmessages:OpenControlChannel",
        "ssmmessages:OpenDataChannel",
      ]
      Resource = "*"
    }]
  })
}

resource "aws_iam_instance_profile" "app" {
  name = "ums-app-ec2"
  role = aws_iam_role.app.name
}
resource "aws_iam_role_policy" "app_parameters" {
  name = "ums-app-read-parameters"
  role = aws_iam_role.app.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = "ssm:GetParameter"
      Resource = [
        "arn:aws:ssm:ap-southeast-1:299482582327:parameter/ums/app/db-password",
        "arn:aws:ssm:ap-southeast-1:299482582327:parameter/ums/app/jwt-secret",
      ]
    }]
  })
}
