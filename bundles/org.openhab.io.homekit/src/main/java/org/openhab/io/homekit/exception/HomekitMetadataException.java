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
 * Exception thrown when there is an error with HomeKit metadata.
 * 
 * This exception is used to indicate problems that occur during the processing
 * or validation of HomeKit metadata, such as invalid metadata values, missing
 * metadata entries, or metadata parsing errors. It extends {@link HomekitFactoryException}
 * to provide specific error handling for metadata-related issues.
 * 
 * Common scenarios where this exception is thrown:
 * - Invalid metadata values
 * - Missing metadata entries
 * - Metadata parsing errors
 * - Metadata update failures
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
public class HomekitMetadataException extends HomekitFactoryException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new HomeKit metadata exception with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public HomekitMetadataException(String message) {
        super(message);
    }

    /**
     * Constructs a new HomeKit metadata exception with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method)
     */
    public HomekitMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
