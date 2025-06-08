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
 * Exception thrown when there is an error during HomeKit service operations.
 * 
 * This exception is used to indicate problems that occur during service
 * management operations, such as service creation, characteristic management,
 * service state changes, or service configuration errors. It extends the base
 * {@link HomekitException} class to provide specific error handling for
 * service-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Service creation failures
 * - Characteristic addition/removal errors
 * - Invalid service state transitions
 * - Service configuration errors
 * - Service type resolution failures
 * - Service instance ID conflicts
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
@NonNullByDefault
public class HomekitServiceException extends HomekitException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new service exception with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public HomekitServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new service exception with the specified detail message and
     * cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */
    public HomekitServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new service exception with the specified cause.
     *
     * @param cause the underlying cause of this exception
     */
    public HomekitServiceException(Throwable cause) {
        super(cause);
    }
}
