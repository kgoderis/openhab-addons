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

/**
 * Unchecked exception for binding-internal failures that should not propagate as checked exceptions
 * through openHAB handler APIs.
 * <p>
 * Typical uses include invalid listener registration (null callback) and unrecoverable gateway I/O
 * errors wrapped from {@link org.openhab.binding.atsadvanced.internal.gateway.AtsGatewayClient}.
 * Panel protocol faults are usually returned as {@code return.error} messages and surfaced via
 * {@link org.openhab.binding.atsadvanced.internal.handler.PanelHandler#getLastError()} rather than
 * thrown exceptions.
 *
 * @author Karel Goderis - Initial contribution
 */
public class ATSAdvancedException extends RuntimeException {

    private static final long serialVersionUID = 2095258016390913221L;

    /**
     * Creates an exception with a descriptive message.
     *
     * @param msg human-readable explanation
     */
    public ATSAdvancedException(String msg) {
        super(msg);
    }

    /**
     * Creates an exception wrapping an underlying cause.
     *
     * @param cause the root failure
     */
    public ATSAdvancedException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with both message and cause.
     *
     * @param msg human-readable explanation
     * @param cause the root failure
     */
    public ATSAdvancedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
