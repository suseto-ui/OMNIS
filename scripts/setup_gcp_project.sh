#!/usr/bin/env bash
# ==============================================================================
# O.M.N.I.S. - Automatizovaný Setup pro Google Cloud Console & GCP Infrastructure
# Rozpočtový strop: < 5.00 USD / měsíc (Zero Cost v nečinnosti)
# ==============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${CYAN}================================================================${NC}"
echo -e "${CYAN}      O.M.N.I.S. • Google Cloud Console Provisioning Engine     ${NC}"
echo -e "${CYAN}================================================================${NC}"

if [ -z "${1:-}" ]; then
  echo -e "${RED}[CHYBA] Zadejte Google Cloud Project ID jako první parametr!${NC}"
  echo "Použití: ./scripts/setup_gcp_project.sh <PROJECT_ID> [REGION=europe-west1] [BILLING_ACCOUNT_ID]"
  exit 1
fi

PROJECT_ID="$1"
REGION="${2:-europe-west1}"
BILLING_ACCOUNT_ID="${3:-}"

echo -e "${GREEN}[1/6] Nastavení aktivního GCP projektu: ${PROJECT_ID}${NC}"
gcloud config set project "$PROJECT_ID"

echo -e "${GREEN}[2/6] Povolení Google Cloud API rozhraní...${NC}"
gcloud services enable \
  run.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  sqladmin.googleapis.com \
  iam.googleapis.com \
  billingbudgets.googleapis.com

echo -e "${GREEN}[3/6] Konfigurace Artifact Registry (omnis-repo)...${NC}"
if ! gcloud artifacts repositories describe omnis-repo --location="$REGION" &>/dev/null; then
  gcloud artifacts repositories create omnis-repo \
    --repository-format=docker \
    --location="$REGION" \
    --description="Docker repozitář pro O.M.N.I.S."
  echo "Repozitář omnis-repo vytvořen."
else
  echo "Repozitář omnis-repo již existuje."
fi

echo -e "${GREEN}[4/6] Vytvoření Service Accountů s principem nejnižších privilegií...${NC}"

# 1. Runtime Service Account pro Cloud Run
RUN_SA="omnis-cloud-run@${PROJECT_ID}.iam.gserviceaccount.com"
if ! gcloud iam service-accounts describe "$RUN_SA" &>/dev/null; then
  gcloud iam service-accounts create omnis-cloud-run \
    --display-name="O.M.N.I.S. Cloud Run Runtime Service Account"
  echo "Service account omnis-cloud-run vytvořen."
fi

# Přidělení rolí pro omnis-cloud-run
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUN_SA}" \
  --role="roles/secretmanager.secretAccessor" &>/dev/null

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUN_SA}" \
  --role="roles/cloudsql.client" &>/dev/null

# 2. CI/CD Service Account pro Cloud Build / GitHub Actions
CI_SA="github-actions-deployer@${PROJECT_ID}.iam.gserviceaccount.com"
if ! gcloud iam service-accounts describe "$CI_SA" &>/dev/null; then
  gcloud iam service-accounts create github-actions-deployer \
    --display-name="O.M.N.I.S. CI/CD Service Account"
fi

for role in roles/artifactregistry.writer roles/run.developer roles/iam.serviceAccountUser roles/secretmanager.secretAccessor; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${CI_SA}" \
    --role="$role" &>/dev/null
done

echo -e "${GREEN}[5/6] Příprava Google Cloud Secret Manageru...${NC}"
for secret in OMNIS_DATABASE_URL JWT_SECRET_KEY GOOGLE_CLIENT_ID GEMINI_API_KEY; do
  if ! gcloud secrets describe "$secret" &>/dev/null; then
    gcloud secrets create "$secret" --replication-policy="automatic"
    echo "Secret '$secret' vytvořen v Secret Manageru."
  else
    echo "Secret '$secret' již existuje."
  fi
done

echo -e "${GREEN}[6/6] Rozpočtová ochrana do 5.00 USD měsíčně...${NC}"
if [ -n "$BILLING_ACCOUNT_ID" ]; then
  echo "Vytvářím rozpočtový alarm pro účet $BILLING_ACCOUNT_ID..."
  gcloud billing budgets create \
    --billing-account="$BILLING_ACCOUNT_ID" \
    --display-name="O.M.N.I.S. Strikni Limit 5 USD" \
    --budget-amount=5.00USD \
    --threshold-rule=percent=0.5 \
    --threshold-rule=percent=0.8 \
    --threshold-rule=percent=1.0 \
    --filter-projects="projects/${PROJECT_ID}" || echo "Budget alert již nastaven nebo vyžaduje administrátorská práva."
else
  echo -e "${YELLOW}[UPOZORNĚNÍ] Nebylo zadáno BILLING_ACCOUNT_ID. Nastavte rozpočet 5 USD manuálně v Google Cloud Console -> Billing -> Budgets & alerts.${NC}"
fi

echo -e "${CYAN}================================================================${NC}"
echo -e "${GREEN}✓ Google Cloud Platform byla úspěšně nakonfigurována pro O.M.N.I.S.!${NC}"
echo -e "${CYAN}================================================================${NC}"
echo "Následující kroky v Google Cloud Console:"
echo "1. Google Cloud Console -> APIs & Services -> Credentials -> Create Credentials -> OAuth Client ID (Web application)."
echo "2. Zkopírujte vygenerovaný 'Client ID' a uložte jej do Secret Manageru (GOOGLE_CLIENT_ID) a v AI Studio panelu Secrets."
echo "3. Vložte do Secret Manageru váš GEMINI_API_KEY a OMNIS_DATABASE_URL."
echo -e "${CYAN}================================================================${NC}"
