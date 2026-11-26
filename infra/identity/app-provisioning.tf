resource "aws_iam_role_policy" "app_provisioning" {
  name = "ums-app-provisioning"
  role = "ums-terraform-network"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadEC2InSingapore"
        Effect = "Allow"
        Action = [
          "ec2:DescribeImages",
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceAttribute",
          "ec2:DescribeInstanceCreditSpecifications",
          "ec2:DescribeInstanceTypes",
          "ec2:DescribeNetworkInterfaces",
          "ec2:DescribeVolumes",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:RequestedRegion" = "ap-southeast-1"
          }
        }
      },
      {
        Sid      = "LaunchTaggedAppInstance"
        Effect   = "Allow"
        Action   = "ec2:RunInstances"
        Resource = "arn:aws:ec2:ap-southeast-1:299482582327:instance/*"
        Condition = {
          StringEquals = {
            "ec2:InstanceType"       = "t3.small"
            "aws:RequestTag/Name"    = "ums-app"
            "aws:RequestTag/Project" = "university-management-system"
          }
        }
      },
      {
        Sid    = "UseAppLaunchResources"
        Effect = "Allow"
        Action = "ec2:RunInstances"
        Resource = [
          "arn:aws:ec2:ap-southeast-1::image/ami-026b70e9abd9fa20f",
          "arn:aws:ec2:ap-southeast-1:299482582327:subnet/subnet-079392bf95ed18e9b",
          "arn:aws:ec2:ap-southeast-1:299482582327:security-group/sg-0eb7af67db4c21699",
          "arn:aws:ec2:ap-southeast-1:299482582327:network-interface/*",
          "arn:aws:ec2:ap-southeast-1:299482582327:volume/*",
        ]
      },
      {
        Sid      = "PassAppRoleToEC2"
        Effect   = "Allow"
        Action   = "iam:PassRole"
        Resource = aws_iam_role.app.arn
        Condition = {
          StringEquals = {
            "iam:PassedToService" = "ec2.amazonaws.com"
          }
        }
      },
    ]
  })
}
