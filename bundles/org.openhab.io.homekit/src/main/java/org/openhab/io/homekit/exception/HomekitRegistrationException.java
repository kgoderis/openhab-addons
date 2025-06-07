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
 * Exception thrown when there is an error during HomeKit service or characteristic registration.
 * 
 * This exception is used to indicate problems that occur during the registration
 * of HomeKit services or characteristics, such as duplicate registrations, invalid
 * registration parameters, or registration failures. It extends {@link HomekitFactoryException}
 * to provide specific error handling for registration-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Duplicate service or characteristic registration
 * - Invalid registration parameters
 * - Registration failures
 * - Resource allocation errors during registration
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitRegistrationException extends HomekitFactoryException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit registration exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitRegistrationException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit registration exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
