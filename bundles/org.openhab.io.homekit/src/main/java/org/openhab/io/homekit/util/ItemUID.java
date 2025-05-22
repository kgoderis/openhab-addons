package org.openhab.io.homekit.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.items.Item;

/**
 * Represents a unique identifier for a Homekit item.
 * This class provides a standardized way to generate and manage unique identifiers
 * for items in the Homekit integration.
 * 
 * The UID format is: homekit:item:{itemName}
 * Example: homekit:item:livingroom_light
 */
@NonNullByDefault
public class ItemUID extends HomekitUID {
    private static final String ITEM_PREFIX = "item";
    private final String itemName;

    /** Wildcard UID that matches any item */
    public static final ItemUID WILDCARD_UID = new ItemUID("*");

    /**
     * Creates a new ItemUID for the specified item.
     *
     * @param item The item to create a UID for
     */
    public ItemUID(Item item) {
        super(ITEM_PREFIX);
        this.itemName = item.getName();
    }

    /**
     * Creates a new ItemUID with the specified item name.
     * The UID will be in the format: homekit:item:{itemName}
     *
     * @param itemName The name of the item
     */
    public ItemUID(String itemName) {
        super(ITEM_PREFIX, itemName);
        this.itemName = itemName;
    }


    /**
     * Returns the name of the item.
     *
     * @return The item name
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Checks if this UID matches a pattern that may contain wildcards.
     * Wildcards can be used in the item name segment.
     *
     * @param pattern The pattern to match against
     * @return true if this UID matches the pattern
     */
    public boolean matches(String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        String[] patternSegments = pattern.split(":");
        if (patternSegments.length != 3) {
            return false;
        }
        if (!patternSegments[0].equals(getPrefix()) || !patternSegments[1].equals(ITEM_PREFIX)) {
            return false;
        }
        String itemPattern = patternSegments[2];
        return itemName.matches(itemPattern.replace("*", ".*"));
    }

    @Override
    protected int getMinimalNumberOfSegments() {
        return 3; // homekit:item:itemName
    }
} 