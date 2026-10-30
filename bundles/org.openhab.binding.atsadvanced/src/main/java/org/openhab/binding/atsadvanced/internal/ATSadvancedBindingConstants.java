/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
 * The {@link ATSadvancedBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Karel Goderis - Initial contribution
 */
public class ATSadvancedBindingConstants {

    public static final String BINDING_ID = "atsadvanced";

    // List of constants used throughout the bindig
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

    // List of all Thing Type UIDs
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

    // List of all Channel ids
    public final static String MONITOR = "monitor";
    public final static String ACTIVE = "active";
    public final static String ALARM = "alarm";
    public final static String SET = "set";
    public final static String FORCE_SET = "forceset";
    public final static String PART_SET = "partset";
    public final static String EXIT = "exit";
    public final static String STATUS = "status";
    public final static String INHIBIT = "inhibit";
    public final static String ISOLATE = "isolate";
    public final static String TAMPER = "tamper";
    public final static String FAULT = "fault";
    public final static String SWITCH = "switch";
    public final static String ONLINE = "online";
    public final static String RESET = "reset";
    public final static String CONFIRM_ALARM = "confirmAlarm";
    public final static String BATTERY_TEST = "batteryTest";
    public final static String WALK_TEST = "walkTest";
    public final static String ARM_PREVIEW = "armPreview";
    public final static String LOG_ENTRY = "logEntry";
    public final static String FETCH_LOG = "fetchLog";
    public final static String LOG_HISTORY = "logHistory";
    public final static String FETCH_LOG_HISTORY = "fetchLogHistory";
    public final static String SCHED_TRIG = "schedTrig";
    /** Max entries kept in {@link #LOG_HISTORY}. */
    public static final int LOG_HISTORY_SIZE = 50;

    // Status flags used by the ATS Advanced Panel
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

    public enum OutputStatusFlags {
        OUTEV_ACTIVE,
        OUTEV_ON
    }

    public enum RasStatusFlags {
        RASEV_OFFLINE,
        RASEV_RTE,
        RASEV_CODETAMPER,
        RASEV_TAMPER
    }

    public enum DgpStatusFlags {
        DGPEV_OFFLINE,
        DGPEV_MAINSFAIL,
        DGPEV_BATTFAIL,
        DGPEV_TAMPER,
        DGPEV_PU_FAIL
    }

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

    public enum PccStatusFlags {
        PCCEV_RUNNING
    }

    public enum CsStatusFlags {
        CSEV_FTC,
        CSEV_HBF,
        CSEV_BUSY
    }

    public enum TriggStatusFlags {
        TRGEV_KEYFOBSW1,
        TRGEV_KEYFOBSW2,
        TRGEV_KEYFOBSW12,
        TRGEV_REMOTEOUT
    }

    public enum UserStatusFlags {
        USEREV_CARDPIN,
        USEREV_SMSCTRLACTIVE,
        USEREV_SMSCTRLLOCK,
        USEREV_SMSREPACTIVE
    }

    public enum UgStatusFlags {
        UGEV_CARDPIN
    }

    public enum FilterStatusFlags {
        CFLEV_ACTIVE
    }

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

    public enum ControlSessionState {

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

        public static ControlSessionState forValue(int value) {
            return typesByValue.get(value);
        }

        public int toInt() {
            return identifier;
        }
    }
}
