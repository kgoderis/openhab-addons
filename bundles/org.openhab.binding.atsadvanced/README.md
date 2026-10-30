# atsadvanced Binding

Control and monitor UTC/FireClass **ATS Advanced** panels from openHAB via an external **dumb gateway** (NAce + `ats.advanced.drv`). Domain logic stays in the Java binding.

## Architecture

- **Gateway** (recommended): .NET 8 process `AtsAdvancedGatewayNet8` — HTTP `/configurepanel`, `/message`, SSE `/events`
- **Binding**: `AtsGatewayClient` (HttpClient + Jackson), no ServiceStack
- **Legacy**: optional Mono spawn of `ATSAdvancedGateway.exe` when `gatewayUrl` is unset

## Thing: `atsadvanced:panel`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ipAddress` | yes | Panel IP |
| `portNumber` | yes | Panel TCP port (default 32000) |
| `pin` | yes | User PIN (one active session per PIN) |
| `gatewayUrl` | no | e.g. `http://127.0.0.1:18137` — preferred |
| `openhab` / `gatewayPortNumber` | no | Legacy Mono gateway addressing |

### Example

```java
Bridge atsadvanced:panel:p1 [
  ipAddress="192.168.0.11",
  portNumber=32000,
  pin=XXXXXX,
  gatewayUrl="http://127.0.0.1:18137"
]
```

## Run the .NET 8 gateway

**Service install** (LaunchAgent / LaunchDaemon / systemd): see [`AtsAdvancedGatewayNet8/deploy/README.md`](AtsAdvancedGatewayNet8/deploy/README.md).

```bash
cd AtsAdvancedGatewayNet8/deploy
./install.sh --agent    # macOS per-user, listens on http://127.0.0.1:18137
```

Manual / POC: `poc/README.md`. On macOS the gateway uses a `kernel32` shim. Build patches NAce `AsyncFireEvent` (`BeginInvoke` → `NetCoreAsyncFireLib`) so panel push events (`msg.MONITOR`, `msgCOS.*`) work on .NET 8.

## Child things

- `atsadvanced:area` — set / force-set / part-set / alarm / exit / status  
- `atsadvanced:zone` — active / alarm / inhibit / isolate / tamper / fault / status  
- `atsadvanced:output` — switch + status (`createCC.OUTPUT`, installer rights typical)  
- `atsadvanced:ras` / `atsadvanced:dgp` — keypad / expander status  

Panel bridge also exposes engineer **reset**, **confirmAlarm**, and **batteryTest**.

### Control sessions (important)

`createCC.ZONE` / `createCC.A_SET` / `createCC.OUTPUT` / … do **not** program the panel. They open a short-lived **command-and-control session**:

1. `createCC.*` → `return.short` session id  
2. one or more `fnCC.*` using that `sessionID` (+ `objectID` for zone/output)  
3. `destroyCC.SESSION`  

Zone inhibit/isolate use a single-state `ZIC_Active` session. Area arm/disarm use the multi-state machines documented in `ats.advanced.pdf`.

Protocol fault/flag prose: `fault-messages.properties` + `ats-prose.properties`. Thing/channel labels use OH-INF i18n.

## Gateway logs in openHAB

Same pattern as the Mono gateway:

1. Gateway emits `INFO:` / `WARNING:` / `ERROR:` on the console and pushes `msg.info` / `msg.warn` / `msg.error` panelevents (property `result`), like `PanelService.logMessage`.
2. Binding logs them as `Gateway [DEBUG]: '…'` / `Gateway [WARNING]: '…'` / `Gateway [ERROR]: '…'` on `PanelHandler`’s logger (legacy Mono process stdout/stderr uses the same prefixes via `GatewayLogHandler`).

```
<Logger name="org.openhab.binding.atsadvanced.internal.handler.PanelHandler" level="DEBUG"/>
```
