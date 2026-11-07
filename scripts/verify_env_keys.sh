#!/usr/bin/env bash
# ==============================================================================
# MOTTU SMART OPS — ENVIRONMENT & SECRETS VERIFICATION SCRIPT
# ==============================================================================
# Usage:
#   ./scripts/verify_env_keys.sh [--env-file .env]
#
# Validates presence, formatting and connectivity readiness for all environment
# variables across Core API, Intelligence Service, MinIO, Postgres, and Redis.
# Fully adheres to Law 8 of AGENTS.md (autonomous offline mode with GEMINI_MOCK_MODE).
# ==============================================================================

set -uo pipefail

ENV_FILE="${1:-.env}"

# Colors for terminal output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

echo -e "${BOLD}${CYAN}===================================================================${NC}"
echo -e "${BOLD}${CYAN}   MOTTU SMART OPS — ENVIRONMENT & SECRETS VERIFICATION GATES     ${NC}"
echo -e "${BOLD}${CYAN}===================================================================${NC}"

if [ -f "$ENV_FILE" ]; then
    echo -e "📄 Loading environment from: ${BOLD}$ENV_FILE${NC}"
    # Export without subshell hazards
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    echo -e "ℹ️  No local ${BOLD}$ENV_FILE${NC} file found; evaluating current shell environment."
fi

ERRORS=0
WARNINGS=0

check_var() {
    local var_name="$1"
    local is_required="${2:-true}"
    local min_length="${3:-1}"
    local val="${!var_name:-}"

    if [ -z "$val" ]; then
        if [ "$is_required" = "true" ]; then
            echo -e "  [${RED}FAIL${NC}] Required variable ${BOLD}$var_name${NC} is MISSING or empty!"
            ERRORS=$((ERRORS + 1))
        else
            echo -e "  [${YELLOW}WARN${NC}] Optional variable ${BOLD}$var_name${NC} is not set."
            WARNINGS=$((WARNINGS + 1))
        fi
    elif [ ${#val} -lt "$min_length" ]; then
        echo -e "  [${RED}FAIL${NC}] Variable ${BOLD}$var_name${NC} is too short (length ${#val} < $min_length)!"
        ERRORS=$((ERRORS + 1))
    else
        echo -e "  [${GREEN} OK ${NC}] ${BOLD}$var_name${NC} is set (${#val} chars)."
    fi
}

echo -e "\n${BOLD}1. Database & Cache Infrastructure:${NC}"
check_var "POSTGRES_DB" "true"
check_var "POSTGRES_USER" "true"
check_var "POSTGRES_PASSWORD" "true"
check_var "REDIS_HOST" "true"
check_var "REDIS_PORT" "true"

echo -e "\n${BOLD}2. Security & Token Configuration:${NC}"
check_var "JWT_SECRET" "true" 32
check_var "INTERNAL_API_TOKEN" "true" 16

echo -e "\n${BOLD}3. Object Storage (MinIO / S3 Presigned URLs):${NC}"
check_var "S3_BUCKET_NAME" "true"
check_var "S3_ACCESS_KEY" "true"
check_var "S3_SECRET_KEY" "true"
check_var "S3_INTERNAL_ENDPOINT" "true"
check_var "S3_PUBLIC_ENDPOINT" "true"

echo -e "\n${BOLD}4. Artificial Intelligence & Gemini Multimodal Worker:${NC}"
MOCK_MODE="${GEMINI_MOCK_MODE:-true}"
if [ "$MOCK_MODE" = "true" ]; then
    echo -e "  [${CYAN}MOCK${NC}] ${BOLD}GEMINI_MOCK_MODE=true${NC} — Running in offline autonomous mode."
    echo -e "         Synthetic multimodal vision & deterministic embeddings active (Law 8 AGENTS.md)."
    if [ -n "${GEMINI_API_KEY:-}" ]; then
        echo -e "         (A real GEMINI_API_KEY is present in env, but mock mode takes precedence until set to false)."
    fi
else
    echo -e "  [${YELLOW}LIVE${NC}] ${BOLD}GEMINI_MOCK_MODE=false${NC} — Live calls to Google GenAI enabled."
    check_var "GEMINI_API_KEY" "true" 20
    if [ -n "${GEMINI_API_KEY:-}" ] && command -v curl &> /dev/null; then
        echo -e "         Testing Gemini API key connectivity..."
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
            "https://generativelanguage.googleapis.com/v1beta/models?key=${GEMINI_API_KEY}" || echo "000")
        if [ "$HTTP_CODE" = "200" ]; then
            echo -e "  [${GREEN} OK ${NC}] Gemini API handshake successful (HTTP 200)."
        else
            echo -e "  [${YELLOW}WARN${NC}] Gemini API returned HTTP $HTTP_CODE. Key may be restricted or expired."
            WARNINGS=$((WARNINGS + 1))
        fi
    fi
fi

echo -e "\n${BOLD}5. Google Cloud BigQuery & BI Export:${NC}"
BQ_ENABLED="${BIGQUERY_ENABLED:-false}"
if [ "$BQ_ENABLED" = "false" ]; then
    echo -e "  [${CYAN}MOCK${NC}] ${BOLD}BIGQUERY_ENABLED=false${NC} — Analytics outbox buffered in PostgreSQL."
else
    echo -e "  [${YELLOW}LIVE${NC}] ${BOLD}BIGQUERY_ENABLED=true${NC} — Real BigQuery NDJSON export enabled."
    check_var "GCP_PROJECT_ID" "true"
    check_var "BIGQUERY_DATASET" "true"
    check_var "BIGQUERY_TABLE" "true"
    if [ -n "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]; then
        if [ -f "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
            echo -e "  [${GREEN} OK ${NC}] GCP service account key file exists at: $GOOGLE_APPLICATION_CREDENTIALS"
        else
            echo -e "  [${RED}FAIL${NC}] GCP credentials file not found at: $GOOGLE_APPLICATION_CREDENTIALS"
            ERRORS=$((ERRORS + 1))
        fi
    else
        echo -e "  [${YELLOW}WARN${NC}] GOOGLE_APPLICATION_CREDENTIALS not specified; relying on ADC or VM metadata."
        WARNINGS=$((WARNINGS + 1))
    fi
fi

echo -e "\n${BOLD}${CYAN}-------------------------------------------------------------------${NC}"
echo -e "Verification Summary: ${GREEN}$((ERRORS == 0 ? 0 : ERRORS)) Errors${NC}, ${YELLOW}$WARNINGS Warnings${NC}"
echo -e "${BOLD}${CYAN}-------------------------------------------------------------------${NC}"

if [ "$ERRORS" -gt 0 ]; then
    echo -e "${RED}${BOLD}❌ ENVIRONMENT VALIDATION FAILED with $ERRORS error(s).${NC}"
    echo -e "Please review your configuration or copy values from .env.example."
    exit 1
else
    echo -e "${GREEN}${BOLD}✅ ALL REQUIRED ENVIRONMENT VARIABLES VERIFIED SUCCESSFULLY!${NC}"
    exit 0
fi

