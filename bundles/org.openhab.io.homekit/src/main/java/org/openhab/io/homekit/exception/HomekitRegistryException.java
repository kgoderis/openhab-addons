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
 * Exception thrown when there is an error during HomeKit registry operations.
 * 
 * This exception is used to indicate problems that occur during registry
 * management operations, such as adding/removing items from registries,
 * registry state inconsistencies, or registry access errors. It extends the
 * base
 * {@link HomekitException} class to provide specific error handling for
 * registry-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Registry add/remove operation failures
 * - Registry state inconsistencies
 * - Concurrent modification errors
 * - Registry access permission errors
 * - Registry corruption or data integrity issues
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitRegistryException extends HomekitException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit registry exception with the specified detail
     * message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitRegistryException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit registry exception with the specified detail message
     * and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
