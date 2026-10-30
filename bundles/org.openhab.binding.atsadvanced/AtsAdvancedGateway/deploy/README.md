# AtsAdvancedGateway — install scaffolding

Installs the dumb HTTP gateway as a background service for the openHAB binding.

The **framework-dependent build** checked into [`../publish/`](../publish/) is copied by default (requires a .NET 8 **runtime**, not the SDK). Use `--rebuild` only when you want to recompile from sources.

| File | Role |
|------|------|
| `atsadvanced-gateway.sh` | Process wrapper (`DOTNET_ROOT`, `ASPNETCORE_URLS`, cwd) |
| `com.openhab.atsadvanced.gateway.agent.plist` | macOS **LaunchAgent** (per-user) |
| `com.openhab.atsadvanced.gateway.plist` | macOS **LaunchDaemon** (system) |
| `atsadvanced-gateway.service` | Linux **systemd** unit |
| `install.sh` / `uninstall.sh` | Install / tear down |
| `atsadvanced-gateway.env.example` | Env reference |

Default listen address: `http://127.0.0.1:18137`.

Architecture (closed-source NAce SDK, `ats.advanced.drv` protocol definition, dumb bridge): see the binding [`README.md`](../../README.md#architecture).

## macOS (recommended: LaunchAgent)

```bash
cd AtsAdvancedGateway/deploy
chmod +x install.sh uninstall.sh atsadvanced-gateway.sh
./install.sh --agent
curl -s http://127.0.0.1:18137/health
```

Prefix defaults to `~/opt/atsadvanced-gateway`. Logs: `~/Library/Logs/atsadvanced-gateway/`.

Rebuild from source instead of using `../publish/`:

```bash
./install.sh --agent --rebuild
```

## macOS LaunchDaemon

```bash
sudo ./install.sh --daemon --user "$(whoami)"
```

Prefix: `/usr/local/opt/atsadvanced-gateway`. Logs: `/usr/local/var/log/`.

## Linux systemd

```bash
sudo ./install.sh --systemd --user openhab
```

## openHAB

```java
Bridge atsadvanced:panel:p1 [
  ipAddress="192.168.0.11",
  portNumber=32000,
  pin=XXXXXX,
  gatewayUrl="http://127.0.0.1:18137"
]
```

## Uninstall

```bash
./uninstall.sh --agent          # or sudo … --daemon / --systemd
./uninstall.sh --agent --purge  # also delete install prefix
```
