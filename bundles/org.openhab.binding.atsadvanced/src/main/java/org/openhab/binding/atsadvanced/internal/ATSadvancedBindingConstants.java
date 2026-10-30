/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.atsadvanced.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Shared identifiers for the ATS Advanced binding: thing types, channel ids, protocol limits, and
 * status-flag enums.
 * <p>
 * The binding implements a <em>dumb-gateway</em> architecture: openHAB Java code speaks HTTP JSON and
 * SSE to {@link org.openhab.binding.atsadvanced.internal.gateway.AtsGatewayClient}, which wraps the
 * proprietary NAce/.drv panel protocol. Flag enum constant names match boolean property ids returned
 * by {@code getSTAT.*} responses; handlers map active flags to channel states via
 * {@link org.openhab.binding.atsadvanced.internal.AtsProse}.
 *
 * @author Karel Goderis - Initial contribution
 */
public class ATSadvancedBindingConstants {

    /** OSGi binding id ({@code atsadvanced}). */
    public static final String BINDING_ID = "atsadvanced";

    /** Upper bounds aligned with typical ATS Advanced panel capacity (discovery loops). */
    public static int MAX_NUMBER_ZONES = 368;
    public static int MAX_NUMBER_AREAS = 8;
    public static int MAX_NUMBER_OUTPUTS = 256;
    public static int MAX_NUMBER_RAS = 32;
    public static int MAX_NUMBER_DGP = 32;
    public static int MAX_NUMBER_USERS = 50;
    public static int MAX_NUMBER_USERGROUPS = 16;
    public static int MAX_NUMBER_CS = 16;
    public static int MAX_NUMBER_PCC = 16;
    public static int MAX_NUMBER_TRIGGERS = 255;
    public static int MAX_NUMBER_FILTERS = 64;
    public static int MAX_NUMBER_DIALLERS = 7;

    /** Bridge thing: one gateway + panel session per instance. */
    public final static @NonNull ThingTypeUID THING_TYPE_PANEL = new ThingTypeUID(BINDING_ID, "panel");
    public final static @NonNull ThingTypeUID THING_TYPE_AREA = new ThingTypeUID(BINDING_ID, "area");
    public final static @NonNull ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");
    public final static @NonNull ThingTypeUID THING_TYPE_OUTPUT = new ThingTypeUID(BINDING_ID, "output");
    public final static @NonNull ThingTypeUID THING_TYPE_RAS = new ThingTypeUID(BINDING_ID, "ras");
    public final static @NonNull ThingTypeUID THING_TYPE_DGP = new ThingTypeUID(BINDING_ID, "dgp");
    public final static @NonNull ThingTypeUID THING_TYPE_SYS = new ThingTypeUID(BINDING_ID, "sys");
    public final static @NonNull ThingTypeUID THING_TYPE_PCC = new ThingTypeUID(BINDING_ID, "pcc");
    public final static @NonNull ThingTypeUID THING_TYPE_CS = new ThingTypeUID(BINDING_ID, "cs");
    public final static @NonNull ThingTypeUID THING_TYPE_TRIGGER = new ThingTypeUID(BINDING_ID, "trigger");
    public final static @NonNull ThingTypeUID THING_TYPE_USER = new ThingTypeUID(BINDING_ID, "user");
    public final static @NonNull ThingTypeUID THING_TYPE_USERGROUP = new ThingTypeUID(BINDING_ID, "usergroup");
    public final static @NonNull ThingTypeUID THING_TYPE_FILTER = new ThingTypeUID(BINDING_ID, "filter");
    public final static @NonNull ThingTypeUID THING_TYPE_DIALLER = new ThingTypeUID(BINDING_ID, "dialler");

    /** Live monitor stream text ({@code msg.MONITOR} on the panel bridge). */
    public final static String MONITOR = "monitor";
    /** Generic “active” indicator on zones/outputs. */
    public final static String ACTIVE = "active";
    /** Alarm summary channel on areas/zones. */
    public final static String ALARM = "alarm";
    /** Full-set (arm) command on areas. */
    public final static String SET = "set";
    /** Force-set command on areas. */
    public final static String FORCE_SET = "forceset";
    /** Part-set command on areas. */
    public final static String PART_SET = "partset";
    /** Exit delay / exit state on areas. */
    public final static String EXIT = "exit";
    /** Human-readable status string (flags summary). */
    public final static String STATUS = "status";
    /** Zone inhibit command. */
    public final static String INHIBIT = "inhibit";
    /** Zone isolate command. */
    public final static String ISOLATE = "isolate";
    /** Tamper indicator. */
    public final static String TAMPER = "tamper";
    /** Fault indicator. */
    public final static String FAULT = "fault";
    /** Output on/off switch. */
    public final static String SWITCH = "switch";
    /** Online / connectivity hint on RAS/DGP things. */
    public final static String ONLINE = "online";
    /** Engineer reset ({@code createCC.ENG_RES}) on panel bridge. */
    public final static String RESET = "reset";
    /** Confirm alarms session on panel bridge. */
    public final static String CONFIRM_ALARM = "confirmAlarm";
    /** Battery test start/stop on panel bridge. */
    public final static String BATTERY_TEST = "batteryTest";
    /** Walk test session on panel bridge. */
    public final static String WALK_TEST = "walkTest";
    /** Pre-arm blocker preview text on areas. */
    public final static String ARM_PREVIEW = "armPreview";
    /** Latest translated log line on panel bridge. */
    public final static String LOG_ENTRY = "logEntry";
    /** Pulse to fetch one older log entry. */
    public final static String FETCH_LOG = "fetchLog";
    /** Newest-first log buffer (multiline string). */
    public final static String LOG_HISTORY = "logHistory";
    /** Reload {@link #LOG_HISTORY} from panel log. */
    public final static String FETCH_LOG_HISTORY = "fetchLogHistory";
    /** Scheduled trigger activate/deactivate on trigger things. */
    public final static String SCHED_TRIG = "schedTrig";
    /** Max entries kept in {@link #LOG_HISTORY}. */
    public static final int LOG_HISTORY_SIZE = 50;

    /**
     * Boolean flags from {@code getSTAT.ZONE} / {@code returnSTAT.ZONE} property ids.
     */
    public enum ZoneStatusFlags {
        ZNEV_ACTIVE,
        ZNEV_TAMPER,
        ZNEV_AM,
        ZNEV_BATTFAIL,
        ZNEV_FAULT,
        ZNEV_DIRTY,
        ZNEV_SVSHORT,
        ZNEV_SVLONG,
        ZNEV_INHIBIT,
        ZNEV_ISOLATE,
        ZNEV_SOAK,
        ZNEV_SET,
        ZNEV_ALARM,
        ZNEV_LEARNED,
        ZNEV_PRELEARNED,
        ZNEV_HELDOPEN,
        ZNEV_INVWT
    }

    /** Output object flags ({@code getSTAT.OUT}). */
    public enum OutputStatusFlags {
        OUTEV_ACTIVE,
        OUTEV_ON
    }

    /** Remote arming station flags ({@code getSTAT.RAS}). */
    public enum RasStatusFlags {
        RASEV_OFFLINE,
        RASEV_RTE,
        RASEV_CODETAMPER,
        RASEV_TAMPER
    }

    /** Data gathering panel / expander flags ({@code getSTAT.DGP}). */
    public enum DgpStatusFlags {
        DGPEV_OFFLINE,
        DGPEV_MAINSFAIL,
        DGPEV_BATTFAIL,
        DGPEV_TAMPER,
        DGPEV_PU_FAIL
    }

    /** System-wide flags ({@code getSTAT.SYS}). */
    public enum SysStatusFlags {
        SYSEV_ALLSET,
        SYSEV_AUTOANS,
        SYSEV_RCONNACTV,
        SYSEV_RCONNFAIL,
        SYSEV_LPRGACTV,
        SYSEV_RPRGACTV,
        SYSEV_TIMECHG,
        SYSEV_SSAVER,
        SYSEV_ISIREN,
        SYSEV_ESIREN,
        SYSEV_STROBE,
        SYSEV_FAULT,
        SYSEV_TAMPER,
        SYSEV_SERVICEIN,
        SYSEV_WALKTST_MODE
    }

    /** PCC channel flags ({@code getSTAT.PCC}). */
    public enum PccStatusFlags {
        PCCEV_RUNNING
    }

    /** Central station flags ({@code getSTAT.CS}). */
    public enum CsStatusFlags {
        CSEV_FTC,
        CSEV_HBF,
        CSEV_BUSY
    }

    /** Trigger / keyfob flags ({@code getSTAT.TRIGG}). */
    public enum TriggStatusFlags {
        TRGEV_KEYFOBSW1,
        TRGEV_KEYFOBSW2,
        TRGEV_KEYFOBSW12,
        TRGEV_REMOTEOUT
    }

    /** User account flags ({@code getSTAT.USER}). */
    public enum UserStatusFlags {
        USEREV_CARDPIN,
        USEREV_SMSCTRLACTIVE,
        USEREV_SMSCTRLLOCK,
        USEREV_SMSREPACTIVE
    }

    /** User group flags ({@code getSTAT.UG}). */
    public enum UgStatusFlags {
        UGEV_CARDPIN
    }

    /** Custom event filter flags ({@code getSTAT.FILTER}). */
    public enum FilterStatusFlags {
        CFLEV_ACTIVE
    }

    /**
     * Area (partition) flags ({@code getSTAT.AREA}). Names encode alarm type and arming mode
     * (full/part/unset) prefixes such as {@code AREV_FS*} vs {@code AREV_US*}.
     */
    public enum AreaStatusFlags {
        AREV_FULLSET,
        AREV_PARTSET,
        AREV_UNSET,
        AREV_ALARM,
        AREV_FSALARM,
        AREV_PSALARM,
        AREV_USALARM,
        AREV_FTCALARM,
        AREV_FIREDOOR,
        AREV_FSFIREDOOR,
        AREV_PSFIREDOOR,
        AREV_USFIREDOOR,
        AREV_FTCFIREDOOR,
        AREV_FIRE,
        AREV_FSFIRE,
        AREV_PSFIRE,
        AREV_USFIRE,
        AREV_FTCFIRE,
        AREV_PANIC,
        AREV_FSPANIC,
        AREV_PSPANIC,
        AREV_USPANIC,
        AREV_FTCPANIC,
        AREV_MEDICAL,
        AREV_FSMEDICAL,
        AREV_PSMEDICAL,
        AREV_USMEDICAL,
        AREV_FTCMEDICAL,
        AREV_TECHNICAL,
        AREV_FSTECHNICAL,
        AREV_PSTECHNICAL,
        AREV_USTECHNICAL,
        AREV_FTCTECHNICAL,
        AREV_TAMPER,
        AREV_FSTAMPER,
        AREV_PSTAMPER,
        AREV_USTAMPER,
        AREV_FTCTAMPER,
        AREV_DOORBELL,
        AREV_PSDOORBELL,
        AREV_USDOORBELL,
        AREV_ZNACTIVE,
        AREV_ZNINHIBIT,
        AREV_ZNISOLATE,
        AREV_ZNFAULT,
        AREV_ZNAM,
        AREV_ZNTAMPER,
        AREV_RASTAMPER,
        AREV_RASFAULT,
        AREV_DGPTAMPER,
        AREV_DGPFAULT,
        AREV_DURESS,
        AREV_FSDURESS,
        AREV_PSDURESS,
        AREV_USDURESS,
        AREV_FTCDURESS,
        AREV_CODETAMPER,
        AREV_ENTRY,
        AREV_EXIT,
        AREV_EXITFAULT,
        AREV_RTS,
        AREV_SETOK,
        AREV_SETFAULT,
        AREV_UNSETOK,
        AREV_ALARMACK,
        AREV_FIRERESET,
        AREV_WALK,
        AREV_WALKZNACTV,
        AREV_AALARM,
        AREV_BALARM,
        AREV_ISIREN,
        AREV_ESIREN,
        AREV_STROBE,
        AREV_BUZZER,
        AREV_AMRESET,
        AREV_PARTSET2,
        AREV_WARNING,
        AREV_AUTOARM,
        AREV_HAALARM,
        AREV_HBALARM
    }

    /**
     * Control-and-command (C&amp;C) session states from {@code statusCC.SESSION} ({@code stateID}).
     * Used while arming, disarming, zone inhibit, outputs, walk test, etc.
     */
    public enum ControlSessionState {

        /** Unrecognized or missing state id. */
        CSMS_UNKNOWN(0),
        CSMS_CC_Ready(256),
        CSMS_CC_CnfAlarms(257),
        CSMS_CC_Confirmed(258),
        CSMS_UC_Ready(768),
        CSMS_UC_Unsetting(769),
        CSMS_UC_CnfAlarms(770),
        CSMS_UC_CnfFaults(771),
        CSMS_UC_Unset(772),
        CSMS_FC_Ready(1280),
        CSMS_FC_Faults(1281),
        CSMS_FC_ActiveStates(1282),
        CSMS_FC_Inhibited(1283),
        CSMS_FC_Setting(1284),
        CSMS_FC_Set(1285),
        CMS_SC_Faults(63000),
        CMS_SC_ActiveStates(63001),
        CMS_SC_Setting(63002);

        private int identifier;

        private ControlSessionState(int value) {
            identifier = value;
        }

        private static final Map<Integer, ControlSessionState> typesByValue = new HashMap<Integer, ControlSessionState>();

        static {
            for (ControlSessionState type : ControlSessionState.values()) {
                typesByValue.put(type.identifier, type);
            }
        }

        /**
         * @param value raw {@code stateID} from the gateway
         * @return mapped state, or {@code null} if unknown
         */
        public static ControlSessionState forValue(int value) {
            return typesByValue.get(value);
        }

        /**
         * @return protocol numeric state id
         */
        public int toInt() {
            return identifier;
        }
    }
}
