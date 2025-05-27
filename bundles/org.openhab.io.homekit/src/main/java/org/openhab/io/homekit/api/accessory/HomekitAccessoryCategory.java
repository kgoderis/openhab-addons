package org.openhab.io.homekit.api.accessory;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing HomeKit accessory categories as defined by Apple's HomeKit Accessory Protocol.
 * <p>
 * This enum provides a standardized way to categorize HomeKit accessories, ensuring compatibility
 * with the HomeKit ecosystem. Each category represents a specific type of device that can be
 * controlled through HomeKit.
 * </p>
 * <p>
 * The categories are used to:
 * <ul>
 *   <li>Classify accessories for HomeKit controllers</li>
 *   <li>Determine available services and characteristics</li>
 *   <li>Group similar devices in the Home app</li>
 *   <li>Ensure proper device representation</li>
 * </ul>
 * </p>
 * <p>
 * Key implementation details:
 * <ul>
 *   <li>Each category has a unique numeric value</li>
 *   <li>Categories are immutable</li>
 *   <li>Unknown values default to OTHER</li>
 *   <li>Reserved values are explicitly marked</li>
 * </ul>
 * </p>
 * <p>
 * The enum integrates with:
 * <ul>
 *   <li>{@link org.openhab.io.homekit.api.accessory.HomekitAccessory} for accessory categorization</li>
 *   <li>{@link org.openhab.io.homekit.api.service.HomekitService} for service availability</li>
 * </ul>
 * </p>
 *
 * @author Karel Goderis - Initial Contribution
 * @since 1.0.0
 */
@NonNullByDefault
public enum HomekitAccessoryCategory {
    OTHER(1, "Other"),
    BRIDGES(2, "Bridges"),
    FANS(3, "Fans"),
    GARAGE_DOOR_OPENERS(4, "Garage Door Openers"),
    LIGHTING(5, "Lighting"),
    LOCKS(6, "Locks"),
    OUTLETS(7, "Outlets"),
    SWITCHES(8, "Switches"),
    THERMOSTATS(9, "Thermostats"),
    SENSORS(10, "Sensors"),
    SECURITY_SYSTEMS(11, "Security Systems"),
    DOORS(12, "Doors"),
    WINDOWS(13, "Windows"),
    WINDOW_COVERINGS(14, "Window Coverings"),
    PROGRAMMABLE_SWITCHES(15, "Programmable Switches"),
    RESERVED_16(16, "Reserved"),
    IP_CAMERAS(17, "IP Cameras"),
    VIDEO_DOORBELLS(18, "Video Doorbells"),
    AIR_PURIFIERS(19, "Air Purifiers"),
    HEATERS(20, "Heaters"),
    AIR_CONDITIONERS(21, "Air Conditioners"),
    HUMIDIFIERS(22, "Humidifiers"),
    DEHUMIDIFIERS(23, "Dehumidifiers"),
    RESERVED_24(24, "Reserved"),
    RESERVED_25(25, "Reserved"),
    RESERVED_26(26, "Reserved"),
    RESERVED_27(27, "Reserved"),
    SPRINKLERS(28, "Sprinklers"),
    FAUCETS(29, "Faucets"),
    SHOWER_SYSTEMS(30, "Shower Systems"),
    RESERVED_31(31, "Reserved"),
    REMOTES(32, "Remotes");

    private final int value;
    private final String description;

    /**
     * Creates a new HomeKit accessory category with the specified value and description.
     *
     * @param value the numeric value of the category
     * @param description the human-readable description of the category
     */
    HomekitAccessoryCategory(int value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Gets the numeric value of this category as defined in the HomeKit protocol.
     * This value is used for protocol communication and device identification.
     *
     * @return the numeric value of this category
     * @since 1.0.0
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets the human-readable description of this category.
     * This description is used for user interface display and documentation.
     *
     * @return the description of this category
     * @since 1.0.0
     */
    public String getDescription() {
        return description;
    }

    /**
     * Converts a numeric value to its corresponding HomeKit accessory category.
     * If the value is not recognized, returns the OTHER category.
     * This method is used to handle unknown or future category values gracefully.
     *
     * @param value the numeric value to convert
     * @return the corresponding HomekitAccessoryCategory, or OTHER if not found
     * @since 1.0.0
     */
    public static HomekitAccessoryCategory fromValue(int value) {
        for (HomekitAccessoryCategory category : values()) {
            if (category.value == value) {
                return category;
            }
        }
        return OTHER; // Default to OTHER for unknown values
    }
}
