package com.clinic.security;

import com.clinic.enums.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which module a controller (or a single handler method) belongs to; a method annotation overrides the class one. GET requests need READ, all others need WRITE.
 * {@code readOpen} lets any signed-in user read (lookups such as doctors and beds used by other modules).
 * {@code adminOnly} restricts the controller to administrators regardless of the permission matrix.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleAccess {

    Module value();

    boolean readOpen() default false;

    boolean adminOnly() default false;
}
