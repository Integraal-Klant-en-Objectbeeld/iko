#!/usr/bin/env bash
#
# wait-for-health.sh — poll a health URL until it reports {"status":"UP"} or a timeout elapses.
#
# Usage:
#   ./scripts/wait-for-health.sh <health-url> [timeout-seconds] [interval-seconds]
#
# Example:
#   ./scripts/wait-for-health.sh http://localhost:9090/actuator/health/readiness
#
# Optionally also polls a second URL for plain reachability (HTTP < 500), used to
# gate on the Keycloak token endpoint before the Playwright suite logs in:
#
#   WAIT_FOR_URL=http://localhost:8082/auth/realms/valtimo/.well-known/openid-configuration \
#     ./scripts/wait-for-health.sh http://localhost:9090/actuator/health/readiness
#
set -euo pipefail

HEALTH_URL="${1:-http://localhost:9090/actuator/health/readiness}"
TIMEOUT="${2:-300}"
INTERVAL="${3:-5}"
EXTRA_URL="${WAIT_FOR_URL:-}"

log() {
    printf '[wait-for-health] %s\n' "$1"
}

# Poll an arbitrary URL until curl gets an HTTP status < 500 (i.e. the service answers).
wait_for_reachable() {
    local url="$1"
    local deadline=$(( $(date +%s) + TIMEOUT ))
    log "waiting for reachability of ${url} (timeout ${TIMEOUT}s)"
    while true; do
        local code
        code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "${url}" || echo 000)"
        if [[ "${code}" =~ ^[1-4][0-9][0-9]$ ]]; then
            log "${url} reachable (HTTP ${code})"
            return 0
        fi
        if (( $(date +%s) >= deadline )); then
            log "ERROR: timed out waiting for ${url} (last HTTP ${code})"
            return 1
        fi
        sleep "${INTERVAL}"
    done
}

# Poll a Spring Boot Actuator health URL until it reports status UP.
wait_for_health_up() {
    local url="$1"
    local deadline=$(( $(date +%s) + TIMEOUT ))
    log "waiting for ${url} to report UP (timeout ${TIMEOUT}s)"
    while true; do
        local body
        body="$(curl -s --max-time 5 "${url}" || echo '')"
        if echo "${body}" | grep -q '"status":"UP"'; then
            log "${url} is UP"
            return 0
        fi
        if (( $(date +%s) >= deadline )); then
            log "ERROR: timed out waiting for ${url} to be UP"
            log "last response: ${body:-<none>}"
            return 1
        fi
        sleep "${INTERVAL}"
    done
}

if [[ -n "${EXTRA_URL}" ]]; then
    wait_for_reachable "${EXTRA_URL}"
fi

wait_for_health_up "${HEALTH_URL}"
