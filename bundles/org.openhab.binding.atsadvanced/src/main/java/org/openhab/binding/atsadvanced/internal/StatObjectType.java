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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;

/**
 * Metadata linking openHAB thing types to ATS Advanced gateway message names for status polling.
 * <p>
 * Each constant describes how {@link org.openhab.binding.atsadvanced.internal.handler.StatObjectHandler}
 * and {@link org.openhab.binding.atsadvanced.internal.handler.PanelHandler} fetch object state via
 * {@code getSTAT.*} / {@code getCOS.*} and react to {@code msgCOS.ALL} flags. Protocol details live
 * in the closed-source NAce stack; this binding only forwards JSON through
 * {@link org.openhab.binding.atsadvanced.internal.gateway.AtsGatewayClient}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public enum StatObjectType {
    /** Single system-wide status object (no COS bitset). */
    SYS(ATSadvancedBindingConstants.THING_TYPE_SYS, "getSTAT.SYS", "returnSTAT.SYS", null, null, "APPOBJ_SYS",
            ATSadvancedBindingConstants.SysStatusFlags.class, 1),
    /** Programmable communication channel instances. */
    PCC(ATSadvancedBindingConstants.THING_TYPE_PCC, "getSTAT.PCC", "returnSTAT.PCC", "getCOS.PCC", "returnCOS.PCC",
            "APPOBJ_PCC", ATSadvancedBindingConstants.PccStatusFlags.class, 16),
    /** Central station / receiver links. */
    CS(ATSadvancedBindingConstants.THING_TYPE_CS, "getSTAT.CS", "returnSTAT.CS", "getCOS.CS", "returnCOS.CS",
            "APPOBJ_CS", ATSadvancedBindingConstants.CsStatusFlags.class, 16),
    /** Trigger / keyfob / remote output objects. */
    TRIGG(ATSadvancedBindingConstants.THING_TYPE_TRIGGER, "getSTAT.TRIGG", "returnSTAT.TRIGG", "getCOS.TRIGG",
            "returnCOS.TRIGG", "APPOBJ_TRIGG", ATSadvancedBindingConstants.TriggStatusFlags.class, 255),
    /** Panel user accounts. */
    USER(ATSadvancedBindingConstants.THING_TYPE_USER, "getSTAT.USER", "returnSTAT.USER", "getCOS.USER",
            "returnCOS.USER", "APPOBJ_USER", ATSadvancedBindingConstants.UserStatusFlags.class, 50),
    /** User groups. */
    UG(ATSadvancedBindingConstants.THING_TYPE_USERGROUP, "getSTAT.UG", "returnSTAT.UG", "getCOS.UG", "returnCOS.UG",
            "APPOBJ_UG", ATSadvancedBindingConstants.UgStatusFlags.class, 16),
    /** Custom event log filters. */
    FILTER(ATSadvancedBindingConstants.THING_TYPE_FILTER, "getSTAT.FILTER", "returnSTAT.FILTER", "getCOS.FILTER",
            "returnCOS.FILTER", "APPOBJ_CEVFLT", ATSadvancedBindingConstants.FilterStatusFlags.class, 64);

    /** openHAB thing type for this object class. */
    public final ThingTypeUID thingType;
    /** Gateway request name for a full status snapshot ({@code /message}). */
    public final String getStat;
    /** Expected response name when {@link #getStat} succeeds. */
    public final String returnStat;
    /** Gateway request for change-of-state bitset, or {@code null} if unsupported. */
    public final @Nullable String getCos;
    /** Expected COS response name, or {@code null}. */
    public final @Nullable String returnCos;
    /** Property id in {@code msgCOS.ALL} that signals changes for this object class. */
    public final String cosFlag;
    /** Enum type whose constants match boolean property ids in the STAT response. */
    public final Class<? extends Enum<?>> flagType;
    /** Highest valid 1-based object index for discovery and validation. */
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

    /**
     * @param uid thing type uid
     * @return matching stat object type, or {@code null}
     */
    public static @Nullable StatObjectType forThingType(ThingTypeUID uid) {
        for (StatObjectType t : values()) {
            if (t.thingType.equals(uid)) {
                return t;
            }
        }
        return null;
    }

    /**
     * @param flag {@code APPOBJ_*} id from {@code msgCOS.ALL}
     * @return stat type for that COS flag, or {@code null}
     */
    public static @Nullable StatObjectType forCosFlag(String flag) {
        for (StatObjectType t : values()) {
            if (t.cosFlag.equals(flag)) {
                return t;
            }
        }
        return null;
    }
}
