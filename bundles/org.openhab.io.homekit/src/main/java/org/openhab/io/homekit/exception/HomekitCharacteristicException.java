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
 * Exception thrown when there is an error during HomeKit characteristic
 * operations.
 * 
 * This exception is used to indicate problems that occur during characteristic
 * management operations, such as characteristic creation, value updates,
 * state changes, or characteristic configuration errors. It extends the base
 * {@link HomekitException} class to provide specific error handling for
 * characteristic-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Characteristic creation failures
 * - Value update errors
 * - Invalid characteristic state transitions
 * - Characteristic configuration errors
 * - Characteristic type resolution failures
 * - Characteristic instance ID conflicts
 * - Item type compatibility errors
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitCharacteristicException extends HomekitException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit characteristic exception with the specified detail
     * message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitCharacteristicException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit characteristic exception with the specified detail
     * message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitCharacteristicException(String message, Throwable cause) {
        super(message, cause);
    }
}
