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
 * Base exception class for all HomeKit integration exceptions.
 * 
 * This class serves as the root exception for the HomeKit integration,
 * providing a common base for all HomeKit-related exceptions. It extends
 * the standard Java Exception class and provides constructors for different
 * error scenarios.
 * 
 * The class integrates with:
 * - {@link org.openhab.io.homekit.exception.HomekitServerException} for server-related errors
 * - {@link org.openhab.io.homekit.exception.HomekitAccessoryOperationException} for accessory operation errors
 * - {@link org.openhab.io.homekit.exception.HomekitConfigurationException} for configuration errors
 * - {@link org.openhab.io.homekit.exception.HomekitEventException} for event handling errors
 * - {@link org.openhab.io.homekit.exception.HomekitFactoryException} for factory-related errors
 * - {@link org.openhab.io.homekit.exception.HomekitMetadataException} for metadata-related errors
 * - {@link org.openhab.io.homekit.exception.HomekitRegistrationException} for registration errors
 * - {@link org.openhab.io.homekit.exception.HomekitInvalidStateTransitionException} for state transition errors
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitException extends Exception {

    private static final long serialVersionUID = -9188483500104140469L;

    /**
     * Constructs a new HomeKit exception with no detail message.
     * 
     * The cause is not initialized, and may subsequently be initialized by a call to
     * {@link #initCause(Throwable) initCause}.
     */
    protected HomekitException() {
    }

    /**
     * Constructs a new HomeKit exception with the specified detail message.
     * 
     * The cause is not initialized, and may subsequently be initialized by a call to
     * {@link #initCause(Throwable) initCause}.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit exception with the specified detail message and cause.
     * 
     * Note that the detail message associated with cause is not automatically
     * incorporated in this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new HomeKit exception with the specified cause.
     * 
     * The detail message is set to {@code (cause == null ? null : cause.toString())}
     * (which typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitException(Throwable cause) {
        super(cause);
    }
}
