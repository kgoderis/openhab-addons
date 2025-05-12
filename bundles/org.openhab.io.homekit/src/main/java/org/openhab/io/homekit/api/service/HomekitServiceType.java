package org.openhab.io.homekit.api.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HomekitServiceType {
    String type(); // The HomeKit service type UUID

    String name() default ""; // Optional human-readable name

    String tag() default "";
}
