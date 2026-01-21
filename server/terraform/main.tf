terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  required_version = ">= 1.2.0"

  backend "s3" {
    bucket         = "battery-butler-tf-state-cartland-dev"
    key            = "server/terraform.tfstate"
    region         = "us-west-1"
    dynamodb_table = "battery-butler-tf-lock"
    encrypt        = true
  }
}

provider "aws" {
  region = "us-west-1"
}
