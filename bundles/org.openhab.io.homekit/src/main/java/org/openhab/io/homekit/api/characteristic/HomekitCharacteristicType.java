package org.openhab.io.homekit.api.characteristic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HomekitCharacteristicType {
    String type(); // The HomeKit characteristic type UUID

    String name() default ""; // Optional human-readable name

    String tag() default "";

    // Binding-specific configurations
    // BindingMapping[] bindings() default {};

    // Fallback channel types if no binding matches
    // String[] channelTypes() default {};
}
