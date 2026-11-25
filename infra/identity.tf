data "aws_caller_identity" "current" {}

output "aws_caller_arn" {
  value = data.aws_caller_identity.current.arn
}
