#!/usr/bin/env bash
# ==============================================================================
# O.M.N.I.S. - Git Synchronizační a Exportní Skript
# Podpora pro GitHub (Personal Access Token) + Google Cloud Repositories
# ==============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}=== [O.M.N.I.S.] Git Synchronizační Nástroj ===${NC}"

# 0. Načtení lokálních proměnných z .env
if [ -f ".env" ]; then
    set -o allexport
    source .env
    set +o allexport
    echo -e "${GREEN}✓ Načteny lokální konfigurační proměnné z .env${NC}"
fi

# 1. Kontrola inicializace Git repozitáře
if [ ! -d ".git" ]; then
    echo -e "${YELLOW}>> Inicializuji lokální Git repozitář...${NC}"
    git init -b main
else
    echo -e "${GREEN}✓ Git repozitář je již inicializován.${NC}"
fi

# 2. Výchozí konfigurace identity
git config user.name "suseto-ui"
git config user.email "susetofu@gmail.com"
echo -e "${GREEN}✓ Nastavena identita uživatele: suseto-ui <susetofu@gmail.com>${NC}"

# 3. Zabezpečený Git Credential Helper
git config --global credential.helper "cache --timeout=7200"

# 4. Načtení GitHub PAT tokenu a repozitáře
GITHUB_TOKEN="${GITHUB_TOKEN:-${1:-}}"
GITHUB_REPO="${GITHUB_REPO:-${2:-https://github.com/suseto-ui/OMNIS.git}}"
GCP_REPO="${GCP_REPO:-${3:-}}"

if [ -n "$GITHUB_TOKEN" ] && [ -n "$GITHUB_REPO" ]; then
    CLEAN_REPO=$(echo "$GITHUB_REPO" | sed -E 's#^https?://##' | sed -E 's#^[^@]+@##')
    AUTH_REMOTE_URL="https://suseto-ui:${GITHUB_TOKEN}@${CLEAN_REPO}"
    
    if git remote get-url origin 2>/dev/null; then
        git remote set-url origin "$AUTH_REMOTE_URL"
    else
        git remote add origin "$AUTH_REMOTE_URL"
    fi
    echo -e "${GREEN}✓ Vzdálený repozitář origin (GitHub: https://${CLEAN_REPO}) nastaven.${NC}"
fi

if [ -n "$GCP_REPO" ]; then
    if git remote get-url google 2>/dev/null; then
        git remote set-url google "$GCP_REPO"
    else
        git remote add google "$GCP_REPO"
    fi
    echo -e "${GREEN}✓ Vzdálený repozitář google (Google Cloud) nastaven.${NC}"
fi

# 5. Staging změn s respektováním .gitignore
echo -e "${YELLOW}>> Přidávám soubory do stagingu...${NC}"
git add .

# 6. Kontrola změn k commitu
if git diff-index --quiet HEAD -- 2>/dev/null; then
    echo -e "${GREEN}✓ Žádné nové změny k commitu. Pracovní strom je čistý.${NC}"
else
    COMMIT_MSG="feat(omnis): full production release - backend cloud run, google auth & android sync"
    git commit -m "$COMMIT_MSG"
    echo -e "${GREEN}✓ Commit vytvořen: $COMMIT_MSG${NC}"
fi

# 7. Push na vzdálené repozitáře
if git remote get-url origin 2>/dev/null; then
    echo -e "${YELLOW}>> Odesílám větve na GitHub (origin/main)...${NC}"
    if git push -u origin main --force 2>&1; then
        echo -e "${GREEN}✓ Úspěšně odesláno na GitHub do suseto-ui/OMNIS!${NC}"
    else
        echo -e "${RED}Varování: Push na GitHub selhal. Zkontrolujte oprávnění.${NC}"
    fi
fi

if git remote get-url google 2>/dev/null; then
    echo -e "${YELLOW}>> Odesílám větve na Google Cloud (google/main)...${NC}"
    git push -u google main || echo -e "${RED}Varování: Push na Google Cloud selhal.${NC}"
fi

echo -e "${GREEN}=== Hotovo. O.M.N.I.S. synchronizace dokončena. ===${NC}"
