/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

package org.openhab.io.homekit.exception;

/**
 * Exception thrown when there is an error with HomeKit server operations.
 * 
 * This exception is used to indicate problems that occur during HomeKit server
 * operations, such as server initialization, client connections, or server shutdown.
 * It extends the base {@link HomekitException} class and provides specific
 * constructors for server-related error scenarios.
 * 
 * Common scenarios where this exception is thrown:
 * - Server initialization failures
 * - Network communication errors
 * - Client connection issues
 * - Server shutdown problems
 * - Protocol violations
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitServerException extends HomekitException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit server exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitServerException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit server exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitServerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new HomeKit server exception with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitServerException(Throwable cause) {
        super(cause);
    }
}
