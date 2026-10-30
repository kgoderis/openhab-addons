/**
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 */
package org.openhab.binding.atsadvanced.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Panel objects that expose getSTAT (+ optional getCOS) for status things.
 */
@NonNullByDefault
public enum StatObjectType {
    SYS(ATSadvancedBindingConstants.THING_TYPE_SYS, "getSTAT.SYS", "returnSTAT.SYS", null, null, "APPOBJ_SYS",
            ATSadvancedBindingConstants.SysStatusFlags.class, 1),
    PCC(ATSadvancedBindingConstants.THING_TYPE_PCC, "getSTAT.PCC", "returnSTAT.PCC", "getCOS.PCC", "returnCOS.PCC",
            "APPOBJ_PCC", ATSadvancedBindingConstants.PccStatusFlags.class, 16),
    CS(ATSadvancedBindingConstants.THING_TYPE_CS, "getSTAT.CS", "returnSTAT.CS", "getCOS.CS", "returnCOS.CS",
            "APPOBJ_CS", ATSadvancedBindingConstants.CsStatusFlags.class, 16),
    TRIGG(ATSadvancedBindingConstants.THING_TYPE_TRIGGER, "getSTAT.TRIGG", "returnSTAT.TRIGG", "getCOS.TRIGG",
            "returnCOS.TRIGG", "APPOBJ_TRIGG", ATSadvancedBindingConstants.TriggStatusFlags.class, 255),
    USER(ATSadvancedBindingConstants.THING_TYPE_USER, "getSTAT.USER", "returnSTAT.USER", "getCOS.USER",
            "returnCOS.USER", "APPOBJ_USER", ATSadvancedBindingConstants.UserStatusFlags.class, 50),
    UG(ATSadvancedBindingConstants.THING_TYPE_USERGROUP, "getSTAT.UG", "returnSTAT.UG", "getCOS.UG", "returnCOS.UG",
            "APPOBJ_UG", ATSadvancedBindingConstants.UgStatusFlags.class, 16),
    FILTER(ATSadvancedBindingConstants.THING_TYPE_FILTER, "getSTAT.FILTER", "returnSTAT.FILTER", "getCOS.FILTER",
            "returnCOS.FILTER", "APPOBJ_CEVFLT", ATSadvancedBindingConstants.FilterStatusFlags.class, 64);

    public final ThingTypeUID thingType;
    public final String getStat;
    public final String returnStat;
    public final @Nullable String getCos;
    public final @Nullable String returnCos;
    public final String cosFlag;
    public final Class<? extends Enum<?>> flagType;
    public final int maxIndex;

    StatObjectType(ThingTypeUID thingType, String getStat, String returnStat, @Nullable String getCos,
            @Nullable String returnCos, String cosFlag, Class<? extends Enum<?>> flagType, int maxIndex) {
        this.thingType = thingType;
        this.getStat = getStat;
        this.returnStat = returnStat;
        this.getCos = getCos;
        this.returnCos = returnCos;
        this.cosFlag = cosFlag;
        this.flagType = flagType;
        this.maxIndex = maxIndex;
    }

    public static @Nullable StatObjectType forThingType(ThingTypeUID uid) {
        for (StatObjectType t : values()) {
            if (t.thingType.equals(uid)) {
                return t;
            }
        }
        return null;
    }

    public static @Nullable StatObjectType forCosFlag(String flag) {
        for (StatObjectType t : values()) {
            if (t.cosFlag.equals(flag)) {
                return t;
            }
        }
        return null;
    }
}
