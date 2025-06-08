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

package org.openhab.io.homekit.network.http;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.io.homekit.api.server.HomekitAccessoryServer;

/**
 * Configuration implementation for HomeKit servlet components.
 *
 * <p>
 * This class implements the ServletConfig interface to provide configuration information
 * for HomeKit servlet components. It maintains references to the HomeKit accessory server,
 * servlet name, and servlet context, while providing a simplified configuration model
 * that doesn't support initialization parameters.
 * </p>
 *
 * <p>
 * <b>Key features:</b>
 * </p>
 * <ul>
 * <li>Implements ServletConfig interface for servlet configuration</li>
 * <li>Provides access to HomeKit accessory server instance</li>
 * <li>Maintains servlet name and context information</li>
 * <li>Simplified configuration without init parameters</li>
 * </ul>
 *
 * <p>
 * <b>Component Integration:</b>
 * </p>
 * <ul>
 * <li>{@link javax.servlet.ServletConfig} for servlet configuration interface</li>
 * <li>{@link javax.servlet.ServletContext} for servlet context information</li>
 * <li>{@link org.openhab.io.homekit.api.server.HomekitAccessoryServer} for HomeKit server functionality</li>
 * </ul>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@NonNullByDefault
public class HomekitServletConfig implements ServletConfig {

    private final HomekitAccessoryServer server;

    private final String servletName;

    private final ServletContext servletContext;

    /**
     * Creates a new HomeKit servlet configuration.
     *
     * <p>
     * This constructor initializes the servlet configuration with the specified
     * HomeKit accessory server, servlet name, and servlet context.
     * </p>
     *
     * <p>
     * <b>Key implementation details:</b>
     * </p>
     * <ul>
     * <li>Stores server instance for later access</li>
     * <li>Initializes servlet name and context</li>
     * <li>Prepares for servlet configuration</li>
     * </ul>
     *
     * @param server The HomeKit accessory server instance
     * @param servletName The name of the servlet
     * @param servletContext The servlet context
     */
    public HomekitServletConfig(HomekitAccessoryServer server, String servletName, ServletContext servletContext) {
        this.server = server;
        this.servletName = servletName;
        this.servletContext = servletContext;
    }

    /**
     * Gets the name of this servlet.
     *
     * <p>
     * This method returns the name assigned to this servlet during initialization.
     * The name is used for identification and logging purposes.
     * </p>
     *
     * @return The servlet name
     */
    @Override
    public String getServletName() {
        return servletName;
    }

    /**
     * Gets the servlet context for this configuration.
     *
     * <p>
     * This method returns the servlet context associated with this configuration.
     * The context provides access to servlet container information and resources.
     * </p>
     *
     * @return The servlet context
     */
    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Gets an initialization parameter value.
     *
     * <p>
     * This implementation always returns null as it doesn't support init parameters.
     * The HomeKit servlet configuration uses a simplified model without initialization
     * parameters.
     * </p>
     *
     * @param name The parameter name
     * @return Always returns null
     */
    @Override
    @SuppressWarnings("null") // Parent ServletConfig interface doesn't constrain this parameter
    public @Nullable String getInitParameter(@Nullable String name) {
        return null;
    }

    /**
     * Gets an empty enumeration of initialization parameter names (internal implementation).
     * 
     * @return An empty enumeration of parameter names
     */
    public Enumeration<String> getEmptyParameterNames() {
        return Collections.emptyEnumeration();
    }

    /**
     * Gets the names of all initialization parameters.
     *
     * <p>
     * This implementation returns an empty enumeration as it doesn't support init parameters.
     * The HomeKit servlet configuration uses a simplified model without initialization
     * parameters.
     * </p>
     *
     * @return An empty enumeration of parameter names
     */
    @Override
    @SuppressWarnings("all") // Framework interface compatibility: ServletConfig interface constraints cannot be overridden
    public java.util.Enumeration<String> getInitParameterNames() {
        return getEmptyParameterNames();
    }

    /**
     * Gets the HomeKit accessory server instance associated with this configuration.
     *
     * <p>
     * This method provides access to the HomeKit accessory server instance that was
     * configured during initialization. The server instance is used for handling
     * HomeKit accessory requests and responses.
     * </p>
     *
     * @return The HomeKit accessory server
     */
    public HomekitAccessoryServer getAccessoryServer() {
        return server;
    }
}
