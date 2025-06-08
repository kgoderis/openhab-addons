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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception thrown when validation or argument errors occur in the HomeKit
 * integration.
 * 
 * This exception is used to indicate that invalid arguments, parameters, or
 * values
 * have been provided to HomeKit components. It serves as a replacement for
 * IllegalArgumentException in HomeKit-specific contexts, providing better error
 * categorization and handling.
 * 
 * Common use cases include:
 * - Invalid characteristic values
 * - Unsupported service or characteristic types
 * - Invalid configuration parameters
 * - Null or empty required parameters
 * - Out-of-range values
 * 
 * The class integrates with the HomeKit exception hierarchy and provides
 * consistent error handling across the integration.
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitValidationException extends HomekitException {

    private static final long serialVersionUID = -2847593847593847593L;

    /**
     * Constructs a new HomeKit validation exception with no detail message.
     * 
     * The cause is not initialized, and may subsequently be initialized by a call
     * to
     * {@link #initCause(Throwable) initCause}.
     */
    public HomekitValidationException() {
        super();
    }

    /**
     * Constructs a new HomeKit validation exception with the specified detail
     * message.
     * 
     * The cause is not initialized, and may subsequently be initialized by a call
     * to
     * {@link #initCause(Throwable) initCause}.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit validation exception with the specified detail
     * message and cause.
     * 
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new HomeKit validation exception with the specified cause.
     * 
     * The detail message is set to
     * {@code (cause == null ? null : cause.toString())}
     * (which typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitValidationException(Throwable cause) {
        super(cause);
    }
}
