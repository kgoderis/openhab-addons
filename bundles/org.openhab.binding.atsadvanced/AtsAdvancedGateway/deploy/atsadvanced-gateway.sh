#!/bin/bash
# Wrapper used by launchd / systemd. Keeps env and working directory consistent.
set -euo pipefail

PREFIX="${ATS_GATEWAY_HOME:-/usr/local/opt/atsadvanced-gateway}"
DOTNET_ROOT="${DOTNET_ROOT:-/opt/homebrew/opt/dotnet@8/libexec}"
# Intel Homebrew / Linux fallbacks
if [[ ! -x "${DOTNET_ROOT}/dotnet" ]]; then
  if [[ -x /usr/local/opt/dotnet@8/libexec/dotnet ]]; then
    DOTNET_ROOT="/usr/local/opt/dotnet@8/libexec"
  elif [[ -x /usr/share/dotnet/dotnet ]]; then
    DOTNET_ROOT="/usr/share/dotnet"
  fi
fi

export DOTNET_ROOT
export PATH="${DOTNET_ROOT}:${PATH}"
export ASPNETCORE_URLS="${ASPNETCORE_URLS:-http://127.0.0.1:18137}"
export DOTNET_ENVIRONMENT="${DOTNET_ENVIRONMENT:-Production}"

cd "${PREFIX}"
exec "${DOTNET_ROOT}/dotnet" "${PREFIX}/AtsAdvancedGateway.dll" "$@"
