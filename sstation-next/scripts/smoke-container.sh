#!/usr/bin/env bash
# TC-115: prove the built image actually serves the app, not just that it compiled.
#
# Assumes the compose stack is already up (see docker-compose.ci.yml). Kept as a script rather
# than inline YAML so it can be run by hand against a local stack when CI disagrees with you.
#
# Usage: BASE_URL=http://localhost:8080 scripts/smoke-container.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.ci.yml)
ADMIN_PASSWORD="${SSTATION_DEMO_ADMIN_PASSWORD:-ci-demo-admin-pw}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"

fail() {
  echo "FAIL: $*" >&2
  echo "--- app logs (last 60) ---" >&2
  "${COMPOSE[@]}" logs --tail=60 app >&2 || true
  exit 1
}

pass() { echo "  ok   $*"; }

app_container() { "${COMPOSE[@]}" ps -q app; }

# 1. The image's own HEALTHCHECK. This is what docker compose depends_on, an ECS task definition
#    and any load balancer key off, so it is the check that decides whether a deploy looks alive.
echo "waiting for the container healthcheck (timeout ${HEALTH_TIMEOUT}s)"
cid="$(app_container)"
[ -n "$cid" ] || fail "no app container is running"

for (( i = 0; i < HEALTH_TIMEOUT; i++ )); do
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$cid")"
  case "$status" in
    healthy)   pass "container reported healthy after ${i}s"; break ;;
    unhealthy) fail "container reported unhealthy after ${i}s" ;;
    none)      fail "image has no HEALTHCHECK — the Dockerfile lost it" ;;
  esac
  sleep 1
done
[ "${status:-}" = "healthy" ] || fail "container still '${status:-unknown}' after ${HEALTH_TIMEOUT}s"

# 2. Health endpoint agrees from outside the container (catches a published-port mistake).
health="$(curl -fsS --max-time 10 "$BASE_URL/actuator/health")" || fail "GET /actuator/health failed"
[ "$health" = '{"status":"UP"}' ] || fail "health is not UP: $health"
pass "/actuator/health is UP"

# 3. The app actually renders.
code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$BASE_URL/login")"
[ "$code" = "200" ] || fail "GET /login returned $code"
pass "/login renders"

# 4. The demo seeder ran and its credentials work — the thing a reviewer will do first.
jar="$(mktemp)"; page="$(mktemp)"
curl -fsS -c "$jar" "$BASE_URL/login" -o "$page"
token="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$page" | head -1)"
[ -n "$token" ] || fail "no CSRF token on the login page"

location="$(curl -s -b "$jar" -c "$jar" -o /dev/null -w '%{redirect_url}' \
  -d "username=admin&password=${ADMIN_PASSWORD}&_csrf=${token}" "$BASE_URL/login")"
case "$location" in
  */login\?error) fail "demo admin could not sign in — did the demo seeder run?" ;;
  "") fail "login did not redirect at all" ;;
esac
pass "demo admin signs in"

code="$(curl -s -b "$jar" -o /dev/null -w '%{http_code}' "$BASE_URL/admin")"
[ "$code" = "200" ] || fail "GET /admin returned $code for a signed-in admin"
pass "/admin renders for that session"

# 5. The dev password must never work on a demo host (TC-114's whole point).
jar2="$(mktemp)"; page2="$(mktemp)"
curl -fsS -c "$jar2" "$BASE_URL/login" -o "$page2"
token2="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$page2" | head -1)"
location="$(curl -s -b "$jar2" -o /dev/null -w '%{redirect_url}' \
  -d "username=admin&password=admin_secret&_csrf=${token2}" "$BASE_URL/login")"
case "$location" in
  */login\?error) pass "admin_secret is rejected" ;;
  *) fail "admin_secret was ACCEPTED on a demo host — TC-114's guarantee is broken" ;;
esac

rm -f "$jar" "$page" "$jar2" "$page2"
echo "container smoke test passed"
