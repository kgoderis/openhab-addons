# ATS Advanced gateway — install scaffolding

Keeps the dumb .NET 8 gateway running outside openHAB (preferred over Mono spawn).

| File | Role |
|------|------|
| `atsadvanced-gateway.sh` | Process wrapper (`DOTNET_ROOT`, `ASPNETCORE_URLS`, cwd) |
| `com.openhab.atsadvanced.gateway.agent.plist` | macOS **LaunchAgent** (per-user) |
| `com.openhab.atsadvanced.gateway.plist` | macOS **LaunchDaemon** (system) |
| `atsadvanced-gateway.service` | Linux **systemd** unit |
| `install.sh` / `uninstall.sh` | Publish + register / tear down |
| `atsadvanced-gateway.env.example` | Env reference |

Default listen address: `http://127.0.0.1:18137`.

Build patches `Library/NAce.Utilities.dll` (from `.dll.orig`) so panel event listeners use `NetCoreAsyncFireLib` instead of Framework `BeginInvoke` — required for `msg.MONITOR` / COS push parity with Mono.


## macOS (recommended: LaunchAgent)

```bash
cd AtsAdvancedGatewayNet8/deploy
chmod +x install.sh uninstall.sh atsadvanced-gateway.sh
./install.sh --agent
curl -s http://127.0.0.1:18137/health
```

Prefix defaults to `~/opt/atsadvanced-gateway`. Logs: `~/Library/Logs/atsadvanced-gateway/`.

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
