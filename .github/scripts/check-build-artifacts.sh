#!/usr/bin/env bash
set -euo pipefail

build_tool="${1:-}"

case "${build_tool}" in
  maven)
    classes_dir="target/classes"
    jacoco_report="target/site/jacoco/jacoco.xml"
    sbom_roots=(target)
    ;;
  gradle)
    classes_dir="build/resources/main"
    jacoco_report="build/reports/jacoco/test/jacocoTestReport.xml"
    sbom_roots=(build/reports)
    ;;
  *)
    echo "Usage: $0 maven|gradle" >&2
    exit 2
    ;;
esac

require_file() {
  local path="$1"
  if [[ ! -s "${path}" ]]; then
    echo "Missing expected build artifact: ${path}" >&2
    exit 1
  fi
}

require_file "${classes_dir}/META-INF/build-info.properties"
require_file "${classes_dir}/git.properties"
require_file "${jacoco_report}"

if ! find "${sbom_roots[@]}" -type f \( -name 'bom.json' -o -name 'bom.xml' -o -name '*.cdx.json' -o -name '*.cdx.xml' -o -name '*cyclonedx*.json' -o -name '*cyclonedx*.xml' \) | grep -q .; then
  echo "Missing expected CycloneDX SBOM artifact under ${sbom_roots[*]}" >&2
  exit 1
fi
