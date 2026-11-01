resource "google_compute_network" "omnis_network" {
  name                    = "omnis-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "omnis_subnet" {
  name          = "omnis-subnet"
  ip_cidr_range = "10.0.0.0/28"
  region        = var.region
  network       = google_compute_network.omnis_network.id
}

resource "google_vpc_access_connector" "connector" {
  name          = "omnis-vpc-conn"
  region        = var.region
  # Cloud SQL private services access is attached to the project's default VPC.
  network       = "default"
  ip_cidr_range = "10.8.0.0/28"
}

# Rezervace privátních IP pro Cloud SQL
resource "google_compute_global_address" "private_ip_address" {
  name          = "omnis-private-ip-address"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.omnis_network.id
}