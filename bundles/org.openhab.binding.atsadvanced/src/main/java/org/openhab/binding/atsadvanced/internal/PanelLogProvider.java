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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.servicestack.client.LogProvider;
import net.servicestack.client.LogType;

/**
 * @author Karel Goderis - Initial contribution
 */
public class PanelLogProvider extends LogProvider {

    private Logger logger = LoggerFactory.getLogger(PanelLogProvider.class);

    @Override
    public void println(LogType type, Object message) {
        logger.trace("{} {} : {}", getPrefix(), logTypeString(type), message);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }
}
