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

/**
 * Configuration of an {@code atsadvanced:panel} bridge thing.
 * <p>
 * {@link #gatewayUrl} points at the dumb gateway; {@link #ipAddress} / {@link #portNumber} are passed to
 * {@code /configurepanel} so the gateway opens the panel TCP session. {@link #pin} is sent as {@code userPIN} on
 * {@code device.getConnect}.
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public class PanelConfiguration {

    /** Panel IP/hostname as reachable from the gateway host. */
    public String ipAddress = "";
    /** Panel protocol port (default 32000). */
    public int portNumber = 32000;
    /** User PIN with monitor/control/log-read privileges. */
    public int pin;
    /** Base URL of AtsAdvancedGateway (e.g. {@code http://127.0.0.1:18137}). */
    public String gatewayUrl = "";
}
