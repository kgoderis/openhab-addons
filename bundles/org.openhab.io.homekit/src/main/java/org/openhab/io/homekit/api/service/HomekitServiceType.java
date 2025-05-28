package org.openhab.io.homekit.api.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark HomeKit service classes and provide metadata about the service.
 * This annotation is used by the {@link org.openhab.io.homekit.api.factory.HomekitServiceFactory} to discover and
 * create service instances.
 *
 * <p>
 * The annotation provides:
 * <ul>
 * <li>Service type identifier</li>
 * <li>Human-readable service name</li>
 * <li>Service tag for identification</li>
 * </ul>
 * </p>
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * {@code
 * @HomekitServiceType(type = "LightBulb", name = "Light Bulb", tag = "light")
 * public class HomekitLightBulbService extends AbstractHomekitService {
 *     // Service implementation
 * }
 * }
 * </pre>
 * </p>
 *
 * <p>
 * The interface integrates with:
 * <ul>
 * <li>{@link org.openhab.io.homekit.api.factory.HomekitServiceFactory} for service creation</li>
 * <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service implementation</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HomekitServiceType {
    /**
     * The type identifier for the service.
     * This is used internally to identify the service type and must be unique.
     *
     * <p>
     * The type should be a simple string identifier that represents the service's
     * functionality, such as "LightBulb", "Switch", or "Thermostat".
     * </p>
     *
     * @return The service type identifier
     * @since 1.0
     */
    String type();

    /**
     * The human-readable name of the service.
     * This is used for display purposes and should be user-friendly.
     *
     * <p>
     * The name should be a descriptive string that clearly indicates the service's
     * purpose, such as "Light Bulb", "Power Switch", or "Temperature Control".
     * </p>
     *
     * @return The service name
     * @since 1.0
     */
    String name();

    /**
     * The tag used to identify the service.
     * This is typically a shorter version of the type used for configuration and mapping.
     *
     * <p>
     * The tag should be:
     * <ul>
     * <li>Lowercase</li>
     * <li>Simple and concise</li>
     * <li>Unique within the system</li>
     * </ul>
     * </p>
     *
     * @return The service tag
     * @since 1.0
     */
    String tag();
}
