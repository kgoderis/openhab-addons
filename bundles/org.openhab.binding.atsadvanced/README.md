# ATS Advanced Binding

This add-on connects openHAB to **UTC / Carrier / FireClass ATS Advanced** intrusion panels. It is split into two parts that must both be installed:

| Part | Language | Location | Role |
|------|----------|----------|------|
| **Binding** | Java | this OSGi bundle (`org.openhab.binding.atsadvanced`) | openHAB things/channels, COS status, C&C sessions, event prose |
| **Gateway** | C# (.NET 8) | [`AtsAdvancedGateway/`](AtsAdvancedGateway/) | Hosts the proprietary NAce stack + `ats.advanced.drv`, exposes a dumb HTTP/SSE API |

## Architecture

ATS Advanced panel access depends on a **closed-source vendor SDK** (NAce assemblies under `AtsAdvancedGateway/Library/`). Those binaries are not open-sourced and cannot be reimplemented inside the Java binding. On top of that stack, the vendor ships **`ats.advanced.drv`**: a driver definition that encodes the **panel protocol** (message names, object types, status flags, C&C sessions, and related wire semantics). Without that `.drv` file the SDK has no protocol map for an ATS Advanced panel.

Because the SDK is .NET-only and closed, this add-on uses a **dumb bridge** design:

| Layer | Responsibility |
|-------|----------------|
| **AtsAdvancedGateway** (C#) | Thin host for NAce + `ats.advanced.drv`. Forwards configure/login/`message` requests and panel push events. **No** openHAB domain logic, no arming policy, no event translation. |
| **Java binding** | All openHAB behaviour: things/channels, COS polling, `createCC.*` / `fnCC.*` sessions, discovery, event prose, logging. Speaks only HTTP/SSE to the gateway. |

```text
openHAB (Java binding)                 AtsAdvancedGateway (.NET)              ATS Advanced panel
  domain logic, things, rules   <-->     dumb HTTP/SSE bridge          <-->     TCP / NAce
                                         + closed-source NAce SDK
                                         + ats.advanced.drv (protocol)
```

The binding never loads NAce or the `.drv` itself. It configures the panel and exchanges protocol messages through the gateway (`POST /configurepanel`, `POST /message`, `GET /events`). The gateway is intentionally “dumb”: it serialises requests to the SDK and streams panel events back; interpretation stays in Java so the open source part of the add-on remains reviewable and portable.

`ats.advanced.drv` is packaged in this repository next to the gateway sources and is copied into the publish output at build time — it must sit beside the running gateway binary.

## Supported Things

| Thing type ID | Type   | Description |
|---------------|--------|-------------|
| `panel`       | Bridge | One panel, via `gatewayUrl` |
| `area`        | Thing  | Area / partition — arm, part-set, force-set, alarm/exit, walk-test, arm preview |
| `zone`        | Thing  | Zone — active/alarm/tamper/fault, inhibit & isolate |
| `output`      | Thing  | Panel output — activate/deactivate |
| `ras`         | Thing  | Remote arming station / keypad status |
| `dgp`         | Thing  | Data gathering panel / expander status |
| `sys`         | Thing  | Panel-wide system status |
| `pcc`         | Thing  | PC connection status (read-only) |
| `cs`          | Thing  | Central station status (read-only) |
| `trigger`     | Thing  | Trigger — remote / scheduled control |
| `user`        | Thing  | User status (read-only) |
| `usergroup`   | Thing  | User group status (read-only) |
| `filter`      | Thing  | Event filter status (read-only) |
| `dialler`     | Thing  | Dialler name listing (read-only) |

## Discovery

The **panel** bridge is **not** auto-discovered — create it manually (UI or `.things`) with `ipAddress`, `portNumber`, `pin`, and `gatewayUrl`.

Once that bridge is **ONLINE** (gateway reachable, panel connected, PIN logged in, status clients set up), the binding discovers panel objects through the gateway by reading name tables (`select.*Names` / chunks) and publishing results to the openHAB Inbox:

| Discovered thing | Source |
|------------------|--------|
| `zone`, `area`, `output`, `ras`, `dgp` | Named object lists on the panel |
| `sys` | Always offered as object `1` (“System”) |
| `pcc`, `cs`, `trigger`, `user`, `usergroup`, `filter`, `dialler` | Named object lists (read-only / status where applicable) |

Behaviour:

- **Background discovery** runs about once a minute while the panel is online.
- **Manual scan** from the UI (“Scan”) triggers the same pass immediately.
- Only objects with a **non-empty programmed name** on the panel are offered (except `sys`).
- Each result carries configuration `number` (and `name` when known). `number` is the representation property, so inbox duplicates for the same object are avoided.
- Discovery is bound to the panel handler (`ThingHandlerService`); there is no separate OSGi registration to configure.

Accept inbox entries in Main UI, or define the same child things under the bridge in a `.things` file (see [Thing Configuration](#thing-configuration)). Things you create manually with the correct `number` match discovered ones.

## Running the Gateway

A **framework-dependent Release build** is checked into [`AtsAdvancedGateway/publish/`](AtsAdvancedGateway/publish/) on GitHub (DLL + `ats.advanced.drv` + NAce libraries + macOS/Linux `kernel32` shims). You only need a [.NET 8 runtime](https://dotnet.microsoft.com/download/dotnet/8.0) on the host — not the SDK — to run it:

```bash
cd AtsAdvancedGateway/publish
export ASPNETCORE_URLS=http://127.0.0.1:18137
dotnet AtsAdvancedGateway.dll
curl -s http://127.0.0.1:18137/health
```

### Service install

LaunchAgent / LaunchDaemon / systemd helpers copy that checked-in `publish/` tree by default:

```bash
cd AtsAdvancedGateway/deploy
./install.sh --agent    # macOS per-user; see deploy/README.md for daemon/systemd
```

Default URL: `http://127.0.0.1:18137`.

### Rebuilding (optional)

Requirements: [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0).

```bash
cd AtsAdvancedGateway
dotnet publish AtsAdvancedGateway.csproj -c Release --self-contained false \
  -p:UseAppHost=false -o ./publish
# or: ./deploy/install.sh --agent --rebuild
```

Build notes:

- Target framework is **net8.0** (runtime requirement; not part of the assembly name).
- Intermediate `bin/` / `obj/` folders are gitignored; only `publish/` is kept in the repository.
- If `Library/NAce.Utilities.dll.orig` is present, the build patches Framework `BeginInvoke` to `NetCoreAsyncFireLib` so panel push events (`msg.MONITOR`, `msgCOS.*`) work on .NET. A pre-patched `NAce.Utilities.dll` is already committed for out-of-the-box builds.

## Thing Configuration

### `panel` (bridge)

| Parameter    | Required | Default | Description |
|--------------|----------|---------|-------------|
| `ipAddress`  | yes      |         | Panel IP / hostname |
| `portNumber` | yes      | `32000` | Panel TCP port |
| `pin`        | yes      |         | User PIN (**one active session per PIN** on the panel) |
| `gatewayUrl` | yes      |         | Base URL of AtsAdvancedGateway, e.g. `http://127.0.0.1:18137` |

```java
Bridge atsadvanced:panel:home "ATS Panel" [
    ipAddress="192.168.0.11",
    portNumber=32000,
    pin=1234,
    gatewayUrl="http://127.0.0.1:18137"
] {
    Thing area area1 "House" [ number=1 ]
    Thing zone zone12 "Front door" [ number=12 ]
    Thing output output3 "Siren" [ number=3 ]
    Thing sys sys1 "System" [ number=1 ]
    Thing trigger trigger1 "Gate" [ number=1 ]
}
```

### Child things

| Parameter | Required | Description |
|-----------|----------|-------------|
| `number`  | yes      | Object index on the panel. For `sys` this is normally `1`. |

## Channels

### Panel (`panel`)

| Channel           | Type   | Read-only | Description |
|-------------------|--------|-----------|-------------|
| `monitor`         | String | yes       | Latest panel monitor event (translated prose) |
| `reset`           | Switch | no        | Engineer reset. Send `ON` |
| `confirmAlarm`    | Switch | no        | Confirm alarms. Send `ON` |
| `batteryTest`     | Switch | no        | Battery test. `ON`=start, `OFF`=cancel |
| `walkTest`        | Switch | no        | Panel walk-test (all areas). `ON`/`OFF` |
| `logEntry`        | String | yes       | Last entry from `select.getLOG` |
| `fetchLog`        | Switch | no        | Read next log entry. Send `ON` |
| `logHistory`      | String | yes       | Last 50 events, newline-separated |
| `fetchLogHistory` | Switch | no        | Reload last 50 events into `logHistory`. Send `ON` |

### Area (`area`)

| Channel      | Type   | Read-only | Description |
|--------------|--------|-----------|-------------|
| `set`        | Switch | no        | Fully set / unset |
| `forceset`   | Switch | no        | Force-set (`ON` only) |
| `partset`    | Switch | no        | Part-set / unset |
| `alarm`      | Switch | yes       | Area in alarm |
| `exit`       | Switch | yes       | Exit timer active |
| `status`     | String | yes       | Active area flags |
| `walkTest`   | Switch | no        | Area walk-test |
| `armPreview` | String | yes       | Inhibit/block list before arming (`REFRESH`) |

### Zone (`zone`)

| Channel   | Type   | Read-only | Description |
|-----------|--------|-----------|-------------|
| `active`  | Switch | yes       | Zone active |
| `alarm`   | Switch | yes       | Zone alarm |
| `inhibit` | Switch | no        | Inhibit / uninhibit |
| `isolate` | Switch | no        | Isolate / unisolate |
| `tamper`  | Switch | yes       | Tamper |
| `fault`   | Switch | yes       | Fault |
| `status`  | String | yes       | Active zone flags |

### Output (`output`)

| Channel  | Type   | Read-only | Description |
|----------|--------|-----------|-------------|
| `switch` | Switch | no        | Activate / deactivate |
| `status` | String | yes       | Status flags |

### RAS / DGP

| Thing | Channels |
|-------|----------|
| `ras` | `online`, `tamper`, `status` |
| `dgp` | `online`, `tamper`, `fault`, `status` |

### System / PCC / CS / user / usergroup / filter / dialler / trigger

| Thing     | Channels |
|-----------|----------|
| `sys`     | `status`, `fault`, `tamper` |
| `pcc`     | `status`, `active` |
| `cs` / `user` / `usergroup` / `filter` / `dialler` | `status` |
| `trigger` | `switch`, `schedTrig`, `status` |

## Control Sessions

`createCC.*` opens a short-lived command-and-control session (not panel programming):

1. `createCC.*` → session id  
2. `fnCC.*` with that session  
3. `destroyCC.SESSION`  

The binding owns this sequence; rules send ordinary Switch / Refresh commands.

## Full Example

### Items

```java
String   ATS_Monitor       "Monitor [%s]"           { channel="atsadvanced:panel:home:monitor" }
String   ATS_LogHistory    "Log history"            { channel="atsadvanced:panel:home:logHistory" }
Switch   ATS_FetchHistory  "Reload log history"     { channel="atsadvanced:panel:home:fetchLogHistory" }
Switch   ATS_ConfirmAlarm  "Confirm alarms"         { channel="atsadvanced:panel:home:confirmAlarm" }

Switch   Area1_Set         "House armed"            { channel="atsadvanced:area:home:area1:set" }
Switch   Area1_PartSet     "House part-set"         { channel="atsadvanced:area:home:area1:partset" }
Switch   Area1_Alarm       "House alarm"            { channel="atsadvanced:area:home:area1:alarm" }
String   Area1_Status      "House status [%s]"      { channel="atsadvanced:area:home:area1:status" }

Switch   Zone1_Active      "Front door"             { channel="atsadvanced:zone:home:zone1:active" }
Switch   Zone1_Inhibit     "Front door inhibit"     { channel="atsadvanced:zone:home:zone1:inhibit" }
String   Zone1_Status      "Front door status [%s]" { channel="atsadvanced:zone:home:zone1:status" }
```

### Sitemap

```perl
sitemap ats label="ATS Advanced" {
    Frame label="Panel" {
        Text item=ATS_Monitor
        Text item=ATS_LogHistory
        Switch item=ATS_FetchHistory
        Switch item=ATS_ConfirmAlarm
    }
    Frame label="House" {
        Switch item=Area1_Set
        Switch item=Area1_PartSet
        Text item=Area1_Status
        Switch item=Area1_Alarm
    }
}
```

## Logging

Translated monitor / log events are written at INFO on the panel handler. Gateway console lines arrive over SSE as `Gateway [DEBUG|WARNING|ERROR]: '…'`.

```xml
<Logger name="org.openhab.binding.atsadvanced.internal.handler.PanelHandler" level="INFO"/>
```

Event maps: `ats-events.properties`. Fault/flag prose: `fault-messages.properties`, `ats-prose.properties`.

## Limitations

- **One concurrent login per PIN** on the panel.
- Output / engineer commands often need an **installer-level** PIN.
- Dialler things are name listing only (no `getSTAT.DL`).
- Device-level isolate for RAS/DGP is not exposed yet.
- The gateway host must run .NET 8 and load NAce + `ats.advanced.drv` (Windows, or macOS/Linux with the included shims).
- Panel programming (zone attributes, user codes, …) is out of scope — runtime status and C&C only.

## Repository Layout

```text
org.openhab.binding.atsadvanced/
├── src/…                 Java binding (openHAB)
├── AtsAdvancedGateway/   C# gateway sources + ats.advanced.drv + NAce Library/
│   ├── publish/          checked-in framework-dependent build (run with dotnet)
│   ├── deploy/           service install scripts
│   └── tools/            NAce BeginInvoke patch helpers
├── doc/                  protocol reference material (not required at runtime)
└── README.md
```
