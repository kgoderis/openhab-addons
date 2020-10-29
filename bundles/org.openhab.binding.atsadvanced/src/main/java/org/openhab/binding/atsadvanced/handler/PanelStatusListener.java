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
package org.openhab.binding.atsadvanced.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link PanelStatusListener} is interface that is to be implemented
 * by all classes that wish to be informed of events happening to an ATS Advanced Panel
 *
 * @author Karel Goderis - Initial contribution
 * @since 2.0.0
 *
 */
@NonNullByDefault
public interface PanelStatusListener {

    /**
     *
     * Called when the connection with the remote panel is lost
     *
     * @param bridge
     */
    public void onBridgeDisconnected(PanelHandler bridge);

    /**
     * Called when the connection with the remote panel is established
     *
     * @param bridge
     */
    public void onBridgeConnected(PanelHandler bridge);
}
