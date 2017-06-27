package com.effectiveosgi.rt.aws.impl;

import org.apache.felix.service.command.Converter;
import org.osgi.service.component.annotations.Component;

import com.amazonaws.services.s3.model.S3ObjectSummary;

@Component(
		property = {
			Converter.CONVERTER_CLASSES + "=com.amazonaws.services.s3.model.S3ObjectSummary"	
		})
public class S3ObjectConverter implements Converter {
	
	private final String ROW_FORMAT = "%-15s | %-11s | % ,15d | %s";

	@Override
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		throw new UnsupportedOperationException(String.format("Conversion from %s to %s is not supported", in != null ? in.getClass().getName() : "<null>", desiredType.getName()));
	}

	@Override
	public CharSequence format(Object target, int level, Converter escape) throws Exception {
		final String result;
		if (target instanceof S3ObjectSummary[]) {
			StringBuilder b = new StringBuilder();
			b.append("Owner           | Storage     |        Size (B) | Key\n");
			b.append("----------------|-------------|-----------------|------------------------------------------------------\n");
			S3ObjectSummary[] oss = (S3ObjectSummary[]) target;
			for (S3ObjectSummary os : oss)
				b.append(format(os, Converter.LINE, escape)).append('\n');
			b.append("Total ").append(oss.length);
			result = b.toString();
		} else if (target instanceof S3ObjectSummary) {
			S3ObjectSummary os = (S3ObjectSummary) target;
			switch (level) {
			case Converter.INSPECT:
			case Converter.LINE:
				result = String.format(ROW_FORMAT, os.getOwner().getDisplayName(), os.getStorageClass(), os.getSize(), os.getKey());
				break;
			case Converter.PART:
				result = String.format("%s:%s", os.getBucketName(), os.getKey());
				break;
			default:
				throw new IllegalArgumentException("Unknown formatting level " + level);
			}
		} else {
			throw new UnsupportedOperationException(String.format("Conversion from %s to String is not supported", target != null ? target.getClass().getName() : "<null>"));
		}
		return result;
	}

}
