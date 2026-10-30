# ATS Advanced POC (target architecture slice)

Proof of concept for the **final architecture**:

- **Dumb gateway** on **.NET 8** (`../AtsAdvancedGatewayNet8`) — NAce + `ats.advanced.drv`, no ATS domain logic
- **Java client** outside openHAB — session, area status/arm-disarm, **end-user prose**
- **No ServiceStack**, **no Mono spawn**, **no openHAB runtime** required for tests

## Ports (this Mac / LAN)

| Role | Address |
|------|---------|
| POC gateway | `http://127.0.0.1:18137` |
| Panel | `192.168.0.11:32000` |
| Live Mono gateway on mm-m2 | leave on `1337` (unused by POC) |

## Run gateway

```bash
export DOTNET_ROOT="/opt/homebrew/opt/dotnet@8/libexec"   # or your SDK 8 install
export PATH="$DOTNET_ROOT:$PATH"
cd ../AtsAdvancedGatewayNet8
dotnet run --urls http://127.0.0.1:18137
```

On macOS/Linux the process loads a Mach-O `kernel32.dll` shim so NAce `OutputDebugString` P/Invoke succeeds. Panel event listeners work on .NET 8 via the build-time NAce patch (`NetCoreAsyncFireLib`); SSE carries `msg.MONITOR` / `msgCOS.*` like Mono.

## Run Java CLI (outside openHAB)

Use a **PIN that is not already logged in** (one active session per PIN).

```bash
cd poc
mvn -q test
mvn -q exec:java -Dexec.args="--gateway http://127.0.0.1:18137 --panel 192.168.0.11:32000 --pin YOUR_FREE_PIN areas"
mvn -q exec:java -Dexec.args="--gateway http://127.0.0.1:18137 --panel 192.168.0.11:32000 --pin YOUR_FREE_PIN status 2"
```

`arm` / `disarm` exist in the CLI for completeness — **do not run them against a live panel** unless explicitly approved.

## Slice

- Configure / login / monitor start / disconnect
- Area names, `getSTAT.AREA` (status read-only in live smoke)
- Set/unset helpers in Java (no auto-inhibit) — not exercised live by default
- Prose via `ats-prose.properties` (e.g. `AREV_FULLSET` → “Fully set (armed)”)

## Tests

`mvn test` runs prose unit tests and an HTTP contract test against an in-process mock gateway (no panel, no openHAB).
