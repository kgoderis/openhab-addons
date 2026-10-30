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
package org.openhab.binding.atsadvanced.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Shared configuration for numbered panel objects (area, zone, output, …).
 * <p>
 * {@link #number} is the 1-based protocol object id sent as {@code objectID} in {@code getSTAT.*} calls.
 * {@link #name} is populated by {@link org.openhab.binding.atsadvanced.internal.discovery.ATSadvancedDiscoveryService}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class AtsObjectConfiguration {

    /** Configuration property key for the object number. */
    public static final String NUMBER = "number";
    /** Configuration property key for the discovered label. */
    public static final String NAME = "name";

    /** 1-based panel object index. */
    public int number;
    /** Human-readable name from panel name tables. */
    public @Nullable String name;
}
