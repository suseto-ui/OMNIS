variable "project_id" {
  type        = string
  description = "Google Cloud Project ID"
}

variable "region" {
  type        = string
  default     = "europe-west1"
}

variable "db_name" {
  type        = string
  default     = "omnis_db"
}