# Service Account pro GitHub Actions CI/CD
resource "google_service_account" "github_actions" {
  account_id   = "github-actions-deployer"
  display_name = "O.M.N.I.S. GitHub Actions Service Account"
}

# Role potřebné pro build a push do Artifact Registry
resource "google_project_iam_member" "ar_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}

# Role pro nasazení do Cloud Run
resource "google_project_iam_member" "run_developer" {
  project = var.project_id
  role    = "roles/run.developer"
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}

# Povolení pro Service Account užívat runtime identitu Cloud Runu
resource "google_project_iam_member" "sa_user" {
  project = var.project_id
  role    = "roles/iam.serviceAccountUser"
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}

# Přístup k Secret Manageru pro Cloud Run (Runtime)
resource "google_secret_manager_secret_iam_member" "secret_access" {
  secret_id = "OMNIS_DATABASE_URL"
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.github_actions.email}"
}

# Role pro správu Cloud SQL (aby mohl CI/CD spouštět migrace)
resource "google_project_iam_member" "sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}

# Povolení pro interaktivní shell (vyžaduje dodatečná oprávnění pro debugování)
resource "google_project_iam_member" "run_session_manager" {
  project = var.project_id
  role    = "roles/run.admin" # Nebo specifičtější role pro vývojáře
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}