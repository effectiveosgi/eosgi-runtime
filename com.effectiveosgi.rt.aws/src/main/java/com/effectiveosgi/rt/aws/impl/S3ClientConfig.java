package com.effectiveosgi.rt.aws.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.amazonaws.regions.Regions;

@ObjectClassDefinition(description = "S3 Client Configuration")
public @interface S3ClientConfig {

	String access_key();

	String _secret_key();
	
	Regions region();

}
