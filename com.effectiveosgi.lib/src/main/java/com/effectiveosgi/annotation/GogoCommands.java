package com.effectiveosgi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.service.component.annotations.ComponentPropertyType;

@ComponentPropertyType
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface GogoCommands {

	String osgi_command_scope();

	String[] osgi_command_function() default {};

	String[] osgi_converter_classes() default {};

}
