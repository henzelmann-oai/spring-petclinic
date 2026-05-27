#!/usr/bin/env bash
set -euo pipefail

base_url="http://127.0.0.1:8080"
port_forward_log="${RUNNER_TEMP:-/tmp}/petclinic-port-forward.log"

cleanup() {
  if [[ -n "${port_forward_pid:-}" ]] && kill -0 "${port_forward_pid}" 2>/dev/null; then
    kill "${port_forward_pid}"
    wait "${port_forward_pid}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

kubectl port-forward service/petclinic 8080:80 >"${port_forward_log}" 2>&1 &
port_forward_pid=$!

port_forward_ready=false
for _ in {1..30}; do
  if curl --silent --show-error --fail --output /dev/null "${base_url}/livez"; then
    port_forward_ready=true
    break
  fi

  if ! kill -0 "${port_forward_pid}" 2>/dev/null; then
    echo "kubectl port-forward exited unexpectedly:" >&2
    cat "${port_forward_log}" >&2
    exit 1
  fi

  sleep 1
done

if [[ "${port_forward_ready}" != "true" ]]; then
  echo "Timed out waiting for service/petclinic port-forward to become reachable:" >&2
  cat "${port_forward_log}" >&2
  exit 1
fi

curl --silent --show-error --fail --location --output /tmp/petclinic-index.html "${base_url}/"
grep -q "PetClinic" /tmp/petclinic-index.html

curl --silent --show-error --fail --output /dev/null "${base_url}/livez"
curl --silent --show-error --fail --output /dev/null "${base_url}/readyz"

actuator_status="$(
  curl --silent --show-error --output /dev/null --write-out "%{http_code}" "${base_url}/actuator/env"
)"

if [[ "${actuator_status}" =~ ^[23] ]]; then
  echo "/actuator/env must not be publicly exposed, got HTTP ${actuator_status}" >&2
  exit 1
fi

echo "Kubernetes deployment smoke checks passed"
