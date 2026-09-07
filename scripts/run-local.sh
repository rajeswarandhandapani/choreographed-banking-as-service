#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f compose.local.yml up -d --wait
identity_ready=false
for attempt in {1..90}; do
  if curl -fsS http://localhost:18089/realms/baas/.well-known/openid-configuration >/dev/null 2>&1; then identity_ready=true; break; fi
  sleep 2
done
if [[ "$identity_ready" != true ]]; then echo 'Keycloak failed to become ready' >&2; exit 1; fi
if [[ "${1:-}" != "--skip-build" ]]; then mvn -B -ntp verify; fi
mkdir -p .local/logs .local/run
pids=()
trap 'if ((${#pids[@]})); then kill "${pids[@]}" 2>/dev/null || true; fi' EXIT INT TERM
modules=(service-discovery account-service user-service payment-service transaction-service notification-service audit-service api-gateway)
ports=(18761 18081 18082 18083 18084 18085 18086 18080)
for index in "${!modules[@]}"; do
  module="${modules[$index]}"
  cp "$module/target/$module-1.0-SNAPSHOT.jar" ".local/run/$module.jar"
  java -Xms64m -Xmx256m -jar ".local/run/$module.jar" \
    --server.port="${ports[$index]}" \
    --eureka.client.service-url.defaultZone=http://localhost:18761/eureka/ \
    --spring.datasource.url='jdbc:mysql://localhost:13306/baas_db?useSSL=false&allowPublicKeyRetrieval=true' \
    --spring.cloud.stream.kafka.binder.brokers=localhost:19092 \
    --spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:18089/realms/baas/protocol/openid-connect/certs \
    > ".local/logs/$module.log" 2>&1 &
  pids+=("$!")
  ready=false
  for attempt in {1..90}; do
    if curl -fsS "http://localhost:${ports[$index]}/actuator/health" >/dev/null 2>&1; then ready=true; break; fi
    if ! kill -0 "${pids[-1]}" 2>/dev/null; then break; fi
    sleep 2
  done
  if [[ "$ready" != true ]]; then echo "$module failed; inspect .local/logs/$module.log" >&2; exit 1; fi
  echo "$module healthy on ${ports[$index]}"
done
echo 'Choreographed banking ready. Gateway: http://localhost:18080. Ctrl+C stops only these JVMs.'
wait
