package org.openhab.io.homekit.api.accessory;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum representing Homekit accessory categories as defined by Apple's Homekit HomekitAccessory Protocol.
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

    HomekitAccessoryCategory(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static HomekitAccessoryCategory fromValue(int value) {
        for (HomekitAccessoryCategory category : values()) {
            if (category.value == value) {
                return category;
            }
        }
        return OTHER; // Default to OTHER for unknown values
    }
}
