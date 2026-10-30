#!/bin/bash
# Publish + install ATS Advanced .NET 8 gateway (launchd or systemd).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LABEL="com.openhab.atsadvanced.gateway"

MODE="" # agent | daemon | systemd
PREFIX_DEFAULT_AGENT="${HOME}/opt/atsadvanced-gateway"
PREFIX_DEFAULT_SYSTEM="/usr/local/opt/atsadvanced-gateway"
PREFIX=""
DOTNET_ROOT="${DOTNET_ROOT:-}"
RUN_USER=""
LOAD=1
# Default: install the framework-dependent build checked in at ../publish/
# --rebuild re-runs dotnet publish into the install prefix.
REBUILD=0
INSTALL_BINARIES=1

usage() {
  cat <<EOF
Usage: $(basename "$0") --agent|--daemon|--systemd [options]

  --agent       macOS LaunchAgent (per-user, no sudo)  [recommended on a login Mac]
  --daemon      macOS LaunchDaemon (system, needs sudo)
  --systemd     Linux systemd unit (needs sudo)

Options:
  --prefix DIR     Install directory
                   agent default:  ${PREFIX_DEFAULT_AGENT}
                   daemon/systemd: ${PREFIX_DEFAULT_SYSTEM}
  --dotnet-root D  DOTNET_ROOT (auto-detected if omitted)
  --user NAME      Run-as user for --daemon / --systemd
  --rebuild        Run dotnet publish into --prefix (otherwise copy ../publish)
  --no-publish     Do not copy/rebuild binaries (reuse existing files in --prefix)
  --no-load        Install files only; do not launchctl/systemctl enable
  -h, --help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --agent) MODE=agent; shift ;;
    --daemon) MODE=daemon; shift ;;
    --systemd) MODE=systemd; shift ;;
    --prefix) PREFIX="$2"; shift 2 ;;
    --dotnet-root) DOTNET_ROOT="$2"; shift 2 ;;
    --user) RUN_USER="$2"; shift 2 ;;
    --rebuild) REBUILD=1; shift ;;
    --no-publish) INSTALL_BINARIES=0; shift ;;
    --no-load) LOAD=0; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "${MODE}" ]]; then
  echo "Pick one of --agent, --daemon, or --systemd" >&2
  usage
  exit 1
fi

detect_dotnet_root() {
  if [[ -n "${DOTNET_ROOT}" && -x "${DOTNET_ROOT}/dotnet" ]]; then
    return
  fi
  for candidate in \
    "/opt/homebrew/opt/dotnet@8/libexec" \
    "/usr/local/opt/dotnet@8/libexec" \
    "/usr/share/dotnet" \
    "${HOME}/.dotnet"
  do
    if [[ -x "${candidate}/dotnet" ]]; then
      DOTNET_ROOT="${candidate}"
      return
    fi
  done
  echo "dotnet not found; set --dotnet-root" >&2
  exit 1
}

detect_dotnet_root

if [[ -z "${PREFIX}" ]]; then
  if [[ "${MODE}" == "agent" ]]; then
    PREFIX="${PREFIX_DEFAULT_AGENT}"
  else
    PREFIX="${PREFIX_DEFAULT_SYSTEM}"
  fi
fi

if [[ "${MODE}" != "agent" && "$(id -u)" -ne 0 ]]; then
  echo "--daemon / --systemd require sudo" >&2
  exit 1
fi

echo "Mode=${MODE} prefix=${PREFIX} rebuild=${REBUILD} dotnet=${DOTNET_ROOT}"

mkdir -p "${PREFIX}"
if [[ "${MODE}" == "agent" ]]; then
  LOG_DIR="${HOME}/Library/Logs/atsadvanced-gateway"
else
  LOG_DIR="/usr/local/var/log"
  if [[ "${MODE}" == "systemd" ]]; then
    LOG_DIR="/var/log"
  fi
fi
mkdir -p "${LOG_DIR}"

if [[ "${INSTALL_BINARIES}" -eq 1 ]]; then
  export DOTNET_ROOT PATH="${DOTNET_ROOT}:${PATH}"
  if [[ "${REBUILD}" -eq 1 ]]; then
    RID=""
    case "$(uname -s)-$(uname -m)" in
      Darwin-arm64) RID=osx-arm64 ;;
      Darwin-x86_64) RID=osx-x64 ;;
      Linux-aarch64|Linux-arm64) RID=linux-arm64 ;;
      Linux-x86_64) RID=linux-x64 ;;
      *) echo "Unsupported platform $(uname -s) $(uname -m)" >&2; exit 1 ;;
    esac
    echo "Rebuilding framework-dependent gateway (rid=${RID})…"
    "${DOTNET_ROOT}/dotnet" publish "${SRC_DIR}/AtsAdvancedGateway.csproj" \
      -c Release -r "${RID}" --self-contained false -p:UseAppHost=false \
      -o "${PREFIX}"
  else
    PUBLISH_DIR="${SRC_DIR}/publish"
    if [[ ! -f "${PUBLISH_DIR}/AtsAdvancedGateway.dll" ]]; then
      echo "Checked-in build missing at ${PUBLISH_DIR}; run with --rebuild or publish first" >&2
      exit 1
    fi
    echo "Installing checked-in build from ${PUBLISH_DIR}…"
    cp -f "${PUBLISH_DIR}/"* "${PREFIX}/"
  fi
fi

install -m 755 "${SCRIPT_DIR}/atsadvanced-gateway.sh" "${PREFIX}/atsadvanced-gateway.sh"
# Keep shim next to the DLL (publish folder / rebuild should already include them)
if [[ -f "${SRC_DIR}/kernel32.dll" ]]; then
  cp -f "${SRC_DIR}/kernel32.dll" "${PREFIX}/kernel32.dll"
fi
if [[ -f "${SRC_DIR}/libkernel32.dll" ]]; then
  cp -f "${SRC_DIR}/libkernel32.dll" "${PREFIX}/libkernel32.dll"
fi
cp -f "${SCRIPT_DIR}/atsadvanced-gateway.env.example" "${PREFIX}/atsadvanced-gateway.env.example"

render_plist() {
  local src="$1" dest="$2"
  local tmp
  tmp="$(mktemp)"
  sed \
    -e "s|ATS_GATEWAY_HOME_PLACEHOLDER|${PREFIX}|g" \
    -e "s|DOTNET_ROOT_PLACEHOLDER|${DOTNET_ROOT}|g" \
    -e "s|LOG_DIR_PLACEHOLDER|${LOG_DIR}|g" \
    "${src}" > "${tmp}"

  if [[ -n "${RUN_USER}" ]]; then
    # Insert UserName before RunAtLoad
    sed -i.bak "s|<!--USER_BLOCK-->|<key>UserName</key><string>${RUN_USER}</string>|" "${tmp}"
    rm -f "${tmp}.bak"
  else
    sed -i.bak "s|<!--USER_BLOCK-->||" "${tmp}"
    rm -f "${tmp}.bak"
  fi

  install -m 644 "${tmp}" "${dest}"
  rm -f "${tmp}"
}

case "${MODE}" in
  agent)
    PLIST_DEST="${HOME}/Library/LaunchAgents/${LABEL}.plist"
    mkdir -p "${HOME}/Library/LaunchAgents"
    render_plist "${SCRIPT_DIR}/com.openhab.atsadvanced.gateway.agent.plist" "${PLIST_DEST}"
    if [[ "${LOAD}" -eq 1 ]]; then
      launchctl bootout "gui/$(id -u)/${LABEL}" 2>/dev/null || true
      launchctl bootstrap "gui/$(id -u)" "${PLIST_DEST}"
      launchctl enable "gui/$(id -u)/${LABEL}" 2>/dev/null || true
      launchctl kickstart -k "gui/$(id -u)/${LABEL}" 2>/dev/null || launchctl start "${LABEL}" || true
    fi
    echo "Installed LaunchAgent: ${PLIST_DEST}"
    ;;
  daemon)
    PLIST_DEST="/Library/LaunchDaemons/${LABEL}.plist"
    render_plist "${SCRIPT_DIR}/com.openhab.atsadvanced.gateway.plist" "${PLIST_DEST}"
    if [[ "${LOAD}" -eq 1 ]]; then
      launchctl bootout "system/${LABEL}" 2>/dev/null || true
      launchctl bootstrap system "${PLIST_DEST}"
      launchctl enable "system/${LABEL}" 2>/dev/null || true
      launchctl kickstart -k "system/${LABEL}" || true
    fi
    echo "Installed LaunchDaemon: ${PLIST_DEST}"
    ;;
  systemd)
    UNIT_DEST="/etc/systemd/system/atsadvanced-gateway.service"
    tmp="$(mktemp)"
    sed \
      -e "s|/usr/local/opt/atsadvanced-gateway|${PREFIX}|g" \
      -e "s|DOTNET_ROOT=/usr/share/dotnet|DOTNET_ROOT=${DOTNET_ROOT}|g" \
      -e "s|User=openhab|User=${RUN_USER:-openhab}|g" \
      -e "s|Group=openhab|Group=${RUN_USER:-openhab}|g" \
      "${SCRIPT_DIR}/atsadvanced-gateway.service" > "${tmp}"
    install -m 644 "${tmp}" "${UNIT_DEST}"
    rm -f "${tmp}"
    if [[ "${LOAD}" -eq 1 ]]; then
      systemctl daemon-reload
      systemctl enable --now atsadvanced-gateway.service
    fi
    echo "Installed systemd unit: ${UNIT_DEST}"
    ;;
esac

echo
echo "Gateway home: ${PREFIX}"
echo "Logs:         ${LOG_DIR}/atsadvanced-gateway.log"
echo "Health:       curl -s http://127.0.0.1:18137/health"
echo "openHAB:      gatewayUrl=\"http://127.0.0.1:18137\""
