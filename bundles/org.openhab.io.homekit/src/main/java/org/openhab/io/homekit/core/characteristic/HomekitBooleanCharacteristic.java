/**
 * Abstract base class for HomeKit characteristics that handle boolean values.
 * This class extends {@link AbstractHomekitCharacteristic} to provide specialized handling for
 * boolean characteristics in the HomeKit protocol.
 *
 * <p>
 * The class implements boolean value management with:
 * <ul>
 *   <li>Boolean value representation and conversion</li>
 *   <li>Integration with OpenHAB's OnOffType states</li>
 *   <li>Support for paired read/write operations</li>
 *   <li>Event notification capabilities</li>
 * </ul>
 * </p>
 *
 * <p>
 * Key features:
 * <ul>
 *   <li>Supports boolean format as per HomeKit specification</li>
 *   <li>Provides paired read/write access by default</li>
 *   <li>Includes event notifications for value changes</li>
 *   <li>Converts between JSON, OpenHAB states, and boolean values</li>
 *   <li>Uses OnOffType for OpenHAB state representation</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class integrates with:
 * <ul>
 *   <li>{@link AbstractHomekitCharacteristic} - Base characteristic functionality</li>
 *   <li>{@link HomekitService} - Service lifecycle management</li>
 *   <li>{@link HomekitEventManager} - Event handling and notifications</li>
 *   <li>{@link org.openhab.core.library.types.OnOffType} - State type for boolean values</li>
 *   <li>{@link org.openhab.core.library.CoreItemFactory} - Item type factory for switches</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial contribution
 * @version 1.0
 * @since 1.0
 */
package org.openhab.io.homekit.core.characteristic;

import java.util.Map;

import javax.json.JsonNumber;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.io.homekit.api.service.HomekitService;
import org.openhab.io.homekit.event.manager.HomekitEventManager;

/**
 * @author Karel Goderis - Initial Contribution
 *
 */
@NonNullByDefault
public abstract class HomekitBooleanCharacteristic extends AbstractHomekitCharacteristic<Boolean> {

    /**
     * Creates a new boolean characteristic with default settings.
     * This constructor initializes the characteristic with boolean format and
     * enables paired read/write access and events.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     */
    public HomekitBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager) {
        super(service, eventManager);
        withFormat("bool").withPairedWrite(true).withPairedRead(true).withEvents(true);
        initializeValue();
    }

    /**
     * Creates a new boolean characteristic from a JSON configuration.
     * This constructor parses the JSON value to initialize the characteristic.
     *
     * @param service the service this characteristic belongs to
     * @param eventManager the event manager for handling notifications
     * @param value the JSON configuration containing characteristic settings
     */
    public HomekitBooleanCharacteristic(HomekitService service, HomekitEventManager eventManager, JsonValue value) {
        super(service, eventManager, value);
        initializeValue();
    }

    /**
     * Indicates that this characteristic is not hidden in the HomeKit interface.
     *
     * @return false, as boolean characteristics are always visible
     */
    @Override
    public boolean isHidden() {
        return false;
    }

    /**
     * Gets the default value for this characteristic.
     *
     * @return false as the default boolean value
     */
    @Override
    public Boolean getDefault() {
        return false;
    }

    /**
     * Converts a JSON value to a boolean.
     * Handles both numeric (non-zero is true) and boolean JSON values.
     *
     * @param value the JSON value to convert
     * @param conversionMap additional conversion parameters
     * @return the converted boolean value
     */
    @Override
    public Boolean toValue(JsonValue value, Map<String, Object> conversionMap) {
        if (value.getValueType().equals(ValueType.NUMBER)) {
            return ((JsonNumber) value).intValue() > 0;
        }
        return value.equals(JsonValue.TRUE);
    }

    /**
     * Converts an OpenHAB state to a boolean.
     * Converts OnOffType states to their boolean equivalents.
     *
     * @param state the OpenHAB state to convert
     * @param conversionMap additional conversion parameters
     * @return the converted boolean value, false if conversion fails
     */
    @Override
    public Boolean toValue(State state, Map<String, Object> conversionMap) {
        OnOffType convertedState = state.as(OnOffType.class);
        if (convertedState == null) {
            return false;
        }
        return convertedState == OnOffType.ON;
    }

    /**
     * Converts a boolean to an OpenHAB state.
     * Converts boolean values to OnOffType states.
     *
     * @param value the boolean value to convert
     * @return the converted OnOffType state
     */
    @Override
    public State toState(Boolean value) {
        return value ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Gets the accepted OpenHAB item type for this characteristic.
     *
     * @return the item type string for switches
     */
    public static String getAcceptedItemType() {
        return CoreItemFactory.SWITCH;
    }
}
