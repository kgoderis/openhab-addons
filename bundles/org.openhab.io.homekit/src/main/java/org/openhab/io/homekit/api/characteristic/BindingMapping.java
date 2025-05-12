package org.openhab.io.homekit.api.characteristic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BindingMapping {
    String bindingId(); // The OpenHAB binding ID

    String[] channelTypes() default {}; // Channel types for this binding

    String[] channelProperties() default {}; // Channel properties for this binding

    String[] channelTags() default {}; // Channel tags for this binding
}
