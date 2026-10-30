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
package org.openhab.binding.atsadvanced.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Notified when {@link PanelHandler} detects an output COS bit ({@code getCOS.OUT} / {@code APPOBJ_OUT}).
 *
 * @author Karel Goderis - Initial contribution
 */
@NonNullByDefault
public interface OutputStatusListener {
    /**
     * @param outputNumber 1-based output index to refresh via {@code getSTAT.OUT}
     */
    void onOutputStateChanged(int outputNumber);
}
