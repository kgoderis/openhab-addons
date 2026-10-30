#!/usr/bin/env python3
"""Regenerate src/main/resources/ats-events.properties from PDF sources.

Sources:
  - AtsAdvancedGateway/doc/ats.advanced.pdf
      event_ID (ev_*), event_source (APPOBJ_*), logType, sessionType, detail enums
  - AtsAdvancedGateway/doc/references/ATSx500A_Installation_EN.pdf
      Appendix B Table 33 SIA Function descriptions

Usage (from bundle root):
  python3 tools/extract-ats-events.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    from pypdf import PdfReader
except ImportError:
    sys.exit("pip install pypdf")

ROOT = Path(__file__).resolve().parents[1]
PROTO = ROOT / "AtsAdvancedGateway/doc/ats.advanced.pdf"
INSTALL = ROOT / "AtsAdvancedGateway/doc/references/ATSx500A_Installation_EN.pdf"
OUT = ROOT / "src/main/resources/ats-events.properties"

# Appendix B Table 33 — Function column (Advisor Advanced reporting codes)
SIA_FUNCTION = {
    "AN": "Detector dirty restore",
    "AR": "AC restore",
    "AS": "Detector dirty",
    "AT": "AC trouble",
    "BA": "Burglary alarm",
    "BB": "Burglary bypass",
    "BC": "Burglary cancel",
    "BJ": "Detector supervision restore",
    "BR": "Burglary restore",
    "BT": "Burglary trouble",
    "BU": "Burglary unbypass",
    "BV": "Alarm confirm",
    "BW": "Confirmed alarm restore",
    "BZ": "Detector supervision",
    "CF": "Forced closing",
    "CG": "Part set",
    "CL": "Closing (set)",
    "EE": "Exit error",
    "ER": "Expansion restore",
    "ET": "Expansion trouble",
    "ES": "Tamper alarm (expander/keypad/audio)",
    "EJ": "Tamper restore (expander/keypad/audio)",
    "FA": "Fire alarm",
    "FB": "Fire bypass",
    "FJ": "Fire trouble restore",
    "FR": "Fire restore",
    "FT": "Fire trouble",
    "FU": "Fire unbypass",
    "FW": "Fire long supervision",
    "HA": "Hold-up alarm",
    "HR": "Hold-up restore",
    "HV": "Hold-up alarm confirm",
    "HW": "Confirmed hold-up restore",
    "JP": "Service in",
    "JR": "Service out",
    "JT": "Time changed",
    "LB": "Local programming begin",
    "LR": "Line restore",
    "LS": "Local programming stop",
    "LT": "Line fault",
    "LU": "Local programming fail",
    "MA": "Medical alarm",
    "MB": "Medical bypass",
    "MJ": "Medical long supervision restore",
    "MR": "Medical restore",
    "MS": "Medical long supervision",
    "MU": "Medical unbypass",
    "OP": "Unset",
    "OR": "Unset from alarm",
    "PA": "Panic alarm",
    "PB": "Panic bypass",
    "PJ": "Panic trouble restore",
    "PR": "Panic restore",
    "PT": "Panic trouble",
    "PU": "Panic unbypass",
    "RB": "Remote programming begin",
    "RP": "Automatic test / ring-in test",
    "RR": "Power up",
    "RS": "Remote programming success",
    "RU": "Remote programming fail",
    "RX": "Manual CS test",
    "TA": "Tamper alarm",
    "TB": "Tamper bypass",
    "TR": "Tamper restore",
    "TT": "KeyBox zone active",
    "TU": "Tamper unbypass",
    "TS": "Walk test start",
    "TE": "Walk test end",
    "UB": "Device/zone isolated",
    "UU": "Device/zone de-isolated",
    "WF": "Walk test fail",
    "WP": "Walk test pass",
    "XH": "RF jamming restore",
    "XQ": "RF jamming",
    "XR": "Detector/fob low battery restore",
    "XT": "Detector/fob low battery",
    "YC": "Communications fail",
    "YK": "Communications restore",
    "YR": "System battery/fuse restore",
    "YS": "Communication trouble (FTC)",
    "YT": "System battery/fuse trouble",
    "YA": "Siren fault",
    "YH": "Siren restore",
    "YP": "Power supply trouble",
    "YQ": "Power supply restored",
    "ZA": "Technical alarm — low temperature",
    "ZB": "Technical bypass — low temperature",
    "ZJ": "Technical long supervision restore — low temperature",
    "ZR": "Technical restore — low temperature",
    "ZS": "Technical long supervision — low temperature",
    "ZU": "Technical unbypass — low temperature",
    "NC": "CS polling fail",
    "NR": "CS polling restore",
    "CP": "Auto set",
    "OA": "Auto unset",
    "OT": "Late close",
    "OK": "Early open",
    "IA": "Engineer reset request",
    "IR": "Engineer reset restore",
    "GA": "Technical alarm — gas",
    "GR": "Technical restore — gas",
    "GB": "Technical bypass — gas",
    "GU": "Technical unbypass — gas",
    "GS": "Technical long supervision — gas",
    "GJ": "Technical long supervision restore — gas",
    "KA": "Technical alarm — high temperature",
    "KR": "Technical restore — high temperature",
    "KB": "Technical bypass — high temperature",
    "KU": "Technical unbypass — high temperature",
    "KS": "Technical long supervision — high temperature",
    "KJ": "Technical long supervision restore — high temperature",
    "WA": "Technical alarm — water",
    "WR": "Technical restore — water",
    "WB": "Technical bypass — water",
    "WU": "Technical unbypass — water",
    "WS": "Technical long supervision — water",
    "WJ": "Technical long supervision restore — water",
    "UA": "Technical zone — general alarm",
    "UR": "Technical zone — general alarm restore",
    "CI": "Autoset fail",
    "DD": "Access denied",
}

# Protocol-only / compound symbols (not plain 2-letter SIA)
EXPLICIT = {
    "ev_NULL": "No event",
    "ev_ARZN": "AC restore (zone)",
    "ev_ARDGP": "AC restore (expander)",
    "ev_ATZN": "AC trouble (zone)",
    "ev_ATDGP": "AC trouble (expander)",
    "ev_BCUSER": "Burglary cancel (user)",
    "ev_BCKEY": "Burglary cancel (key)",
    "ev_BCPC": "Burglary cancel (PC)",
    "ev_CFUSER": "Forced closing (user)",
    "ev_CFKEY": "Forced closing (key)",
    "ev_CFPC": "Forced closing (PC)",
    "ev_CFRAS": "Forced closing (keypad)",
    "ev_CGUSER": "Part set (user)",
    "ev_CGKEY": "Part set (key)",
    "ev_CGPC": "Part set (PC)",
    "ev_CGRAS": "Part set (keypad)",
    "ev_CLUSER": "Closing / set (user)",
    "ev_CLKEY": "Closing / set (key)",
    "ev_CLPC": "Closing / set (PC)",
    "ev_CLRAS": "Closing / set (keypad)",
    "ev_EEUSER": "Exit error (user)",
    "ev_EESYST": "Exit error (system)",
    "ev_EEZONE": "Exit error (zone)",
    "ev_ERDGP": "Expansion restore (expander)",
    "ev_ERDGPC": "Expansion communication restore (expander)",
    "ev_ERDGPF": "Expansion fuse restore (expander)",
    "ev_ERRAS": "Expansion restore (keypad)",
    "ev_ERRASC": "Expansion communication restore (keypad)",
    "ev_ETDGP": "Expansion trouble (expander)",
    "ev_ETDGPC": "Expansion communication fault (expander)",
    "ev_ETDGPF": "Expansion fuse failure (expander)",
    "ev_ETRAS": "Expansion trouble (keypad)",
    "ev_ETRASC": "Expansion communication fault (keypad)",
    "ev_OPUSER": "Unset (user)",
    "ev_OPKEY": "Unset (key)",
    "ev_OPPC": "Unset (PC)",
    "ev_ORUSER": "Unset from alarm (user)",
    "ev_ORKEY": "Unset from alarm (key)",
    "ev_ORPC": "Unset from alarm (PC)",
    "ev_RRDGP": "Power up (expander)",
    "ev_RRRAS": "Power up (keypad)",
    "ev_TADGP": "Tamper alarm (expander)",
    "ev_TADGPS": "Tamper alarm (expander siren)",
    "ev_TARAS": "Tamper alarm (keypad)",
    "ev_TRDGP": "Tamper restore (expander)",
    "ev_TRDGPS": "Tamper restore (expander siren)",
    "ev_TRRAS": "Tamper restore (keypad)",
    "ev_UBDGP": "Expander isolated",
    "ev_UBRAS": "Keypad isolated",
    "ev_UBZN": "Zone isolated",
    "ev_UUDGP": "Expander de-isolated",
    "ev_UURAS": "Keypad de-isolated",
    "ev_UUZN": "Zone de-isolated",
    "ev_OUTA": "Output activated",
    "ev_OUTR": "Output deactivated",
    "ev_USRG": "User logon",
    "ev_USRD": "User logoff",
    "ev_EROK": "Engineer reset OK",
    "ev_INTEG": "Integrity check",
    "ev_WDG": "Watchdog",
    "ev_CHKSUM": "Checksum error",
    "ev_USRADD": "User added",
    "ev_USRDEL": "User deleted",
    "ev_USRPIN": "User PIN changed",
    "ev_USRTAMP": "User tamper",
    "ev_ZNNTST": "Zone not tested",
    "ev_BJS": "Detector supervision restore (special)",
    "ev_BZS": "Detector supervision (special)",
    "ev_INSTRST": "Installer reset",
    "ev_KBTR": "KeyBox tamper restore",
    "ev_YTL": "Line fault (local)",
    "ev_OVRD": "Override",
    "ev_LTETH": "Line fault (Ethernet)",
    "ev_LTIP": "Line fault (IP)",
    "ev_LRIP": "Line restore (IP)",
    "ev_JTSYS": "Time changed (system)",
    "ev_YSSYS": "Communication trouble / FTC (system)",
    "ev_YKSYS": "Communications restore (system)",
    "ev_SMSUN": "SMS unknown / denied",
    "ev_SMSLIM": "SMS limit reached",
    "ev_YKMI": "Communications restore (modem/interface)",
    "ev_LTGPRS": "Line fault (GPRS)",
    "ev_LTIPGPRS": "Line fault (IP/GPRS)",
    "ev_LRIPGPRS": "Line restore (IP/GPRS)",
    "ev_AASTOPU": "Auto-arm stop (user)",
    "ev_AASTOPS": "Auto-arm stop (system)",
    "ev_PARAS": "Panic alarm (keypad)",
    "ev_PAUSR": "Panic alarm (user)",
    "ev_CA": "Closing abort",
    "ev_CR": "Closing restore",
    "ev_FARAS": "Fire alarm (keypad)",
    "ev_MARAS": "Medical alarm (keypad)",
    "ev_FRRAS": "Fire restore (keypad)",
    "ev_MRRAS": "Medical restore (keypad)",
    "ev_PRRAS": "Panic restore (keypad)",
    "ev_PRUSR": "Panic restore (user)",
    "ev_PICRQ": "Picture requested",
    "ev_PICTKN": "Picture taken",
    "ev_PICSNT": "Picture sent",
    "ev_OHKM": "On-hook / modem",
    "ev_PICGPRSLIM": "Picture GPRS limit",
    "ev_PICETHLIM": "Picture Ethernet limit",
    "ev_PICLIM": "Picture limit",
    "ev_PEBMEMFULL": "Picture memory full",
    "ev_PEBMEMWRC90P": "Picture memory 90% full",
    "ev_GETIMGFAIL": "Get image failed",
    "ev_PICDEL": "Picture deleted",
    "ev_CAMMODEFAIL": "Camera mode failed",
    "ev_BRAM": "Burglary restore (anti-mask)",
    "ev_BTAM": "Burglary trouble (anti-mask)",
    "ev_BRFT": "Burglary restore (fault)",
    "ev_BTFT": "Burglary trouble (fault)",
    "ev_PICMMSLIM": "Picture MMS limit",
    "ev_TSDGP": "Walk test start (expander)",
    "ev_TEDGP": "Walk test end (expander)",
    "ev_TSSYS": "Walk test start (system)",
    "ev_TESYS": "Walk test end (system)",
    "ev_YPDGP": "Power supply trouble (expander)",
    "ev_YQDGP": "Power supply restored (expander)",
}

SOURCE_LABEL = {
    "APPOBJ_ZN": "Zone",
    "APPOBJ_AREA": "Area",
    "APPOBJ_RAS": "Keypad (RAS)",
    "APPOBJ_DGP": "Expander (DGP)",
    "APPOBJ_DGP0": "Panel (DGP0)",
    "APPOBJ_USER": "User",
    "APPOBJ_OUT": "Output",
    "APPOBJ_CEVFLT": "Event filter",
    "APPOBJ_UG": "User group",
    "APPOBJ_SYS": "System",
    "APPOBJ_CS": "Central station",
    "APPOBJ_EV": "Event",
    "APPOBJ_DL": "Dialler",
    "APPOBJ_PCC": "PC connection",
    "APPOBJ_MISC": "Miscellaneous",
    "APPOBJ_TRIGGER": "Trigger",
    "APPOBJ_SCHDL_EXC": "Schedule exception",
    "APPOBJ_SCHDL_CAL": "Schedule calendar",
    "APPOBJ_FOB": "Keyfob",
    "APPOBJ_CAMERA": "Camera",
}

LOG_LABEL = {
    "MANDATORY": "Mandatory",
    "NON_MANDATORY": "Non-mandatory",
    "INSTALLER": "Installer",
    "ACCESS": "Access",
}
SESS_LABEL = {"LOCAL": "Local", "REMOTE": "Remote", "KEYFOB": "Keyfob", "CARD": "Card", "SMS": "SMS"}
SUFFIX = {
    "USER": "user",
    "KEY": "key",
    "PC": "PC",
    "RAS": "keypad",
    "DGP": "expander",
    "ZN": "zone",
    "SYST": "system",
    "SYS": "system",
    "ZONE": "zone",
}


def describe(sym: str) -> str:
    if sym in EXPLICIT:
        return EXPLICIT[sym]
    code = sym[3:] if sym.startswith("ev_") else sym
    if code in SIA_FUNCTION:
        return SIA_FUNCTION[code]
    for suf in sorted(SUFFIX, key=len, reverse=True):
        if code.endswith(suf) and len(code) > len(suf):
            base = code[: -len(suf)]
            if base in SIA_FUNCTION:
                return f"{SIA_FUNCTION[base]} ({SUFFIX[suf]})"
    if len(code) >= 2 and code[:2] in SIA_FUNCTION:
        rest = code[2:]
        return f"{SIA_FUNCTION[code[:2]]} ({rest.lower()})" if rest else SIA_FUNCTION[code[:2]]
    return code.replace("_", " ")


def human_token(s: str) -> str:
    return s.replace("_", " ").capitalize()


def main() -> None:
    if not PROTO.is_file() or not INSTALL.is_file():
        sys.exit(f"Missing PDFs under {ROOT}/AtsAdvancedGateway/doc")

    proto = PdfReader(str(PROTO))
    events: dict[int, str] = {}
    sources: dict[int, str] = {}
    log_types: dict[int, str] = {}
    session_types: dict[int, str] = {}
    fuse: dict[int, str] = {}
    siren: dict[int, str] = {}
    tamper: dict[int, str] = {}
    sms: dict[int, str] = {}
    picsrc: dict[int, str] = {}
    cam: dict[int, str] = {}

    for p in proto.pages:
        t = p.extract_text() or ""
        for m in re.finditer(r"(?m)^(\d+)\s+(ev_[A-Z0-9]+)\b", t):
            events[int(m.group(1))] = m.group(2)
        for m in re.finditer(r"(?m)^(\d+)\s+(APPOBJ_[A-Z0-9_]+)\b", t):
            sources[int(m.group(1))] = m.group(2)
        for m in re.finditer(r"(?m)^(\d+)\s+(MANDATORY|NON_MANDATORY|INSTALLER|ACCESS)\b", t):
            log_types[int(m.group(1))] = m.group(2)
        if "sessionType" in t:
            for m in re.finditer(r"(?m)^(\d+)\s+(LOCAL|REMOTE|KEYFOB|CARD|SMS)\b", t):
                session_types[int(m.group(1))] = m.group(2)
        if "detailsFuse" in t:
            for m in re.finditer(
                r"(?m)^(\d+)\s+(Internal_siren|External_siren|Battery|Communication|Aux_power_supply|DGP)\b", t
            ):
                fuse[int(m.group(1))] = m.group(2)
        if "detailsSiren" in t or "On_board_siren_tamper_input" in t:
            for m in re.finditer(
                r"(?m)^(\d+)\s+(Internal_siren|External_siren|On_board_siren_tamper_input)\b", t
            ):
                siren[int(m.group(1))] = m.group(2)
        if "detailsTamper" in t or "Panel_tamper" in t:
            for m in re.finditer(r"(?m)^(\d+)\s+(Panel_tamper|Siren_tamper)\b", t):
                tamper[int(m.group(1))] = m.group(2)
        if "detailsSmsCnt" in t:
            for m in re.finditer(r"(?m)^(\d+)\s+(Reporting|Forwarding)\b", t):
                sms[int(m.group(1))] = m.group(2)
        if "detailsPictureSource" in t or "TEST_CALL" in t:
            for m in re.finditer(r"(?m)^(\d+)\s+(ZONE|FILTER|TEST_CALL|WALKTEST|SMS|CC|SIA)\b", t):
                picsrc[int(m.group(1))] = m.group(2)
        if "detailsCameraMode" in t or "DAY_ISOLATE" in t:
            for m in re.finditer(
                r"(?m)^(\d+)\s+(DAY|NIGHT|WALKTEST|RANGETEST|LEGACY|ISOLATE|DAY_ISOLATE|NIGHT_ISOLATE|WALKTEST_ISOLATE|RANGETEST_ISOLATE|LEGACY_ISOLATE)\b",
                t,
            ):
                cam[int(m.group(1))] = m.group(2)

    if len(events) != 220 or max(events) != 219:
        sys.exit(f"Expected event_ID 0..219, got {len(events)} max={max(events) if events else None}")

    lines = [
        "# ATS Advanced monitor/log event map (generated by tools/extract-ats-events.py).",
        "# Sources:",
        "#   - ats.advanced.pdf — event_ID / event_source / logType / sessionType / detail enums",
        "#   - ATSx500A_Installation_EN.pdf Appendix B Table 33 — SIA Function descriptions",
        "",
        "# --- event_ID (0..219) ---",
    ]
    for i in range(220):
        sym = events[i]
        lines.append(f"event.{i}={describe(sym)}")
        lines.append(f"eventSymbol.{i}={sym}")

    lines += ["", "# --- event_source (APPOBJ_*) ---"]
    for k in sorted(sources):
        sym = sources[k]
        lines.append(f"source.{k}={SOURCE_LABEL.get(sym, human_token(sym))}")
        lines.append(f"sourceSymbol.{k}={sym}")

    lines += ["", "# --- logType ---"]
    for k in sorted(log_types):
        lines.append(f"logType.{k}={LOG_LABEL.get(log_types[k], log_types[k])}")
        lines.append(f"logTypeSymbol.{k}={log_types[k]}")

    lines += ["", "# --- sessionType ---"]
    for k in sorted(session_types):
        lines.append(f"sessionType.{k}={SESS_LABEL.get(session_types[k], session_types[k])}")
        lines.append(f"sessionTypeSymbol.{k}={session_types[k]}")

    lines += ["", "# --- detail enums ---"]
    for k, v in sorted(fuse.items()):
        lines.append(f"detailsFuse.{k}={human_token(v)}")
    for k, v in sorted(siren.items()):
        lines.append(f"detailsSiren.{k}={human_token(v)}")
    for k, v in sorted(tamper.items()):
        lines.append(f"detailsTamper.{k}={human_token(v)}")
    for k, v in sorted(sms.items()):
        lines.append(f"detailsSmsCnt.{k}={v}")
    for k, v in sorted(picsrc.items()):
        lines.append(f"detailsPictureSource.{k}={human_token(v)}")
    for k, v in sorted(cam.items()):
        lines.append(f"detailsCameraMode.{k}={human_token(v)}")

    lines += ["", "# --- SIA base codes (Appendix B Function) ---"]
    for k in sorted(SIA_FUNCTION):
        lines.append(f"sia.{k}={SIA_FUNCTION[k]}")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUT} ({len(events)} events, {len(sources)} sources, {len(SIA_FUNCTION)} SIA)")


if __name__ == "__main__":
    main()
