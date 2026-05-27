#!/usr/bin/env bash
set -euo pipefail

check_checksum() {
  local file=$1

  if ! grep -Eq '^distributionSha256Sum=[0-9a-f]{64}$' "$file"; then
    echo "Missing or invalid distributionSha256Sum in $file" >&2
    exit 1
  fi
}

check_checksum ".mvn/wrapper/maven-wrapper.properties"
check_checksum "gradle/wrapper/gradle-wrapper.properties"
