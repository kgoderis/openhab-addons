package org.openhab.io.homekit.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.io.homekit.api.hap.Accessory;
import org.openhab.io.homekit.api.hap.Characteristic;
import org.openhab.io.homekit.api.hap.Service;

/**
 * Utility class for comparing accessories, services, and characteristics.
 */
public class AccessoryComparator {
    
    /**
     * Compares two accessories for equality.
     * 
     * @param a1 the first accessory
     * @param a2 the second accessory
     * @return true if the accessories are equal, false otherwise
     */
    public static boolean equals(Accessory a1, Accessory a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        
        return Objects.equals(a1.getUID(), a2.getUID()) &&
               a1.getAccessoryId() == a2.getAccessoryId() &&
               Objects.equals(a1.getLabel(), a2.getLabel()) &&
               Objects.equals(a1.getSerialNumber(), a2.getSerialNumber()) &&
               Objects.equals(a1.getModel(), a2.getModel()) &&
               Objects.equals(a1.getManufacturer(), a2.getManufacturer()) &&
               equals(a1.getServices(), a2.getServices());
    }
    
    /**
     * Compares two collections of services for equality.
     * 
     * @param s1 the first collection of services
     * @param s2 the second collection of services
     * @return true if the service collections are equal, false otherwise
     */
    public static boolean equals(Collection<Service> s1, Collection<Service> s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null || s1.size() != s2.size()) {
            return false;
        }
        
        for (Service service1 : s1) {
            boolean found = false;
            for (Service service2 : s2) {
                if (equals(service1, service2)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Compares two services for equality.
     * 
     * @param s1 the first service
     * @param s2 the second service
     * @return true if the services are equal, false otherwise
     */
    public static boolean equals(Service s1, Service s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        
        return Objects.equals(s1.getUID(), s2.getUID()) &&
               s1.getInstanceId() == s2.getInstanceId() &&
               Objects.equals(s1.getName(), s2.getName()) &&
               Objects.equals(s1.getInstanceType(), s2.getInstanceType()) &&
               s1.isHidden() == s2.isHidden() &&
               s1.isPrimary() == s2.isPrimary() &&
               equals(s1.getCharacteristics(), s2.getCharacteristics());
    }
    
    /**
     * Compares two lists of characteristics for equality.
     * 
     * @param c1 the first list of characteristics
     * @param c2 the second list of characteristics
     * @return true if the characteristic lists are equal, false otherwise
     */
    public static boolean equals(List<Characteristic<?>> c1, List<Characteristic<?>> c2) {
        if (c1 == c2) {
            return true;
        }
        if (c1 == null || c2 == null || c1.size() != c2.size()) {
            return false;
        }
        
        for (int i = 0; i < c1.size(); i++) {
            if (!equals(c1.get(i), c2.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Compares two characteristics for equality.
     * 
     * @param c1 the first characteristic
     * @param c2 the second characteristic
     * @return true if the characteristics are equal, false otherwise
     */
    public static boolean equals(Characteristic<?> c1, Characteristic<?> c2) {
        if (c1 == c2) {
            return true;
        }
        if (c1 == null || c2 == null) {
            return false;
        }
        
        try {
            return Objects.equals(c1.getUID(), c2.getUID()) &&
                   c1.getInstanceId() == c2.getInstanceId() &&
                   Objects.equals(c1.getInstanceType(), c2.getInstanceType()) &&
                   Objects.equals(c1.getValue(), c2.getValue()) &&
                   c1.isHidden() == c2.isHidden();
        } catch (Exception e) {
            return false;
        }
    }
} 