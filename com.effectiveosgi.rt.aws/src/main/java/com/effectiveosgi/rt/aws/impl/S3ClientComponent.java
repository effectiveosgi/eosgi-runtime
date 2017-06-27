package com.effectiveosgi.rt.aws.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.annotations.Designate;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.effectiveosgi.rt.aws.S3ObjectSpliterator;

@Component(
		name = S3ClientComponent.PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		service = Object.class,
		immediate = true,
		property = {
				"osgi.command.scope=s3",
				"osgi.command.function=ls"
		})
@Designate(ocd = S3ClientConfig.class, factory = true)
public class S3ClientComponent {
	
	static final String PID = "com.effectiveosgi.rt.aws.s3";

	private AmazonS3 s3;
	private ServiceRegistration<AmazonS3> svcReg;
	
	@Reference
	LogService log;

	@Activate
	void activate(BundleContext context, final S3ClientConfig config, final Map<String, Object> configProps) {
		// Connect to S3
		AWSCredentialsProvider credentialsProvider = new AWSCredentialsProvider() {
			@Override
			public AWSCredentials getCredentials() {
				return new BasicAWSCredentials(config.access_key(), config._secret_key());
			}
			@Override
			public void refresh() {
			}
		};

		log.log(LogService.LOG_INFO, String.format("Logging into Amazon S3 region %s using basic credentials: %s:***", config.region() != null ? config.region().getName() : "<default>", config.access_key()));
		AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider);
		if (config.region() != null)
			clientBuilder = clientBuilder.withRegion(config.region());
		s3 = clientBuilder.build();
		log.log(LogService.LOG_INFO, "Logged into Amazon S3");

		// Publish service
		Dictionary<String, Object> svcProps = new Hashtable<>(configProps);
		for (Enumeration<String> keyEnum = svcProps.keys(); keyEnum.hasMoreElements(); ) {
			String key = keyEnum.nextElement();
			if (key.startsWith(".")) svcProps.remove(key);
		}
		svcReg = context.registerService(AmazonS3.class, s3, svcProps);
	}

	@Deactivate
	void deactivate() {
		svcReg.unregister();
		s3.shutdown();
		log.log(LogService.LOG_INFO, "Logged out from Amazon S3");
	}

	public S3ObjectSummary[] ls(@Descriptor("bucket") String bucket) {
		return ls(bucket, "");
	}

	public S3ObjectSummary[] ls(@Descriptor("bucket") String bucket, @Descriptor("path prefix") String path) {
		return StreamSupport.stream(new S3ObjectSpliterator(s3, bucket, path), false).collect(Collectors.toList()).toArray(new S3ObjectSummary[0]);
	}

}
