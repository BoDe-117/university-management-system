provider "aws" {
  region = "ap-southeast-1"
}
provider "aws" {
  alias               = "database"
  region              = "ap-southeast-1"
  profile             = "ums-database-terraform"
  allowed_account_ids = ["299482582327"]
}
