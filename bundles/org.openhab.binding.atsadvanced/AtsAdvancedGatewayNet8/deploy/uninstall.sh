#!/bin/bash
# Unload and remove ATS Advanced gateway launchd / systemd registration (keeps --prefix bits unless --purge).
set -euo pipefail

LABEL="com.openhab.atsadvanced.gateway"
MODE=""
PURGE=0
PREFIX=""

usage() {
  cat <<EOF
Usage: $(basename "$0") --agent|--daemon|--systemd [--purge] [--prefix DIR]

  --purge   Also delete the install prefix
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --agent) MODE=agent; shift ;;
    --daemon) MODE=daemon; shift ;;
    --systemd) MODE=systemd; shift ;;
    --purge) PURGE=1; shift ;;
    --prefix) PREFIX="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "${MODE}" ]]; then
  usage
  exit 1
fi

case "${MODE}" in
  agent)
    launchctl bootout "gui/$(id -u)/${LABEL}" 2>/dev/null || launchctl unload "${HOME}/Library/LaunchAgents/${LABEL}.plist" 2>/dev/null || true
    rm -f "${HOME}/Library/LaunchAgents/${LABEL}.plist"
    PREFIX="${PREFIX:-${HOME}/opt/atsadvanced-gateway}"
    ;;
  daemon)
    if [[ "$(id -u)" -ne 0 ]]; then echo "need sudo" >&2; exit 1; fi
    launchctl bootout "system/${LABEL}" 2>/dev/null || launchctl unload "/Library/LaunchDaemons/${LABEL}.plist" 2>/dev/null || true
    rm -f "/Library/LaunchDaemons/${LABEL}.plist"
    PREFIX="${PREFIX:-/usr/local/opt/atsadvanced-gateway}"
    ;;
  systemd)
    if [[ "$(id -u)" -ne 0 ]]; then echo "need sudo" >&2; exit 1; fi
    systemctl disable --now atsadvanced-gateway.service 2>/dev/null || true
    rm -f /etc/systemd/system/atsadvanced-gateway.service
    systemctl daemon-reload
    PREFIX="${PREFIX:-/usr/local/opt/atsadvanced-gateway}"
    ;;
esac

if [[ "${PURGE}" -eq 1 && -n "${PREFIX}" && -d "${PREFIX}" ]]; then
  rm -rf "${PREFIX}"
  echo "Removed ${PREFIX}"
fi

echo "Uninstalled ${MODE} service ${LABEL}"
