package org.openhab.io.homekit.network.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

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
 * @author Karel Goderis - Initial Contribution
 * @since 1.0
 */
public class HomekitServletConfig implements ServletConfig {

    /** The HomeKit accessory server instance */
    private final HomekitAccessoryServer server;

    /** The name of this servlet */
    private final String servletName;

    /** The servlet context for this configuration */
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
    public String getInitParameter(String name) {
        return null;
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
    public java.util.Enumeration<String> getInitParameterNames() {
        return java.util.Collections.emptyEnumeration();
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
