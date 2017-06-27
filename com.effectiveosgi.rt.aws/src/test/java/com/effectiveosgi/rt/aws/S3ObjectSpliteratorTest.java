package com.effectiveosgi.rt.aws;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;


public class S3ObjectSpliteratorTest extends EasyMockSupport {

	private static final String BUCKET = "mybucket";

	@Test
	public void test() {
		// Create some dummy keys
		String[] keys = new String[10];
		for (int i = 0; i < keys.length; i++) keys[i] = "key"+i;

		// Create some s3 objects
		S3ObjectSummary[] objects = new S3ObjectSummary[10];
		for (int i = 0; i < objects.length; i++) {
			objects[i] = new S3ObjectSummary();
			objects[i].setBucketName(BUCKET);
			objects[i].setKey(keys[i]);
		}

		// Create page 1 with keys 0-2
		ObjectListing listing1 = new ObjectListing();
		listing1.getObjectSummaries().clear();
		for (int i = 0; i < 3; i++) listing1.getObjectSummaries().add(objects[i]);
		listing1.setTruncated(true);

		// Create page 2 with keys 3-5
		ObjectListing listing2 = new ObjectListing();
		listing2.getObjectSummaries().clear();
		for (int i = 3; i < 6; i++) listing2.getObjectSummaries().add(objects[i]);
		listing2.setTruncated(true);
		
		// Create page 3 with keys 6-9
		ObjectListing listing3 = new ObjectListing();
		listing3.getObjectSummaries().clear();
		for (int i = 6; i < 10; i++) listing3.getObjectSummaries().add(objects[i]);
		listing3.setTruncated(false);
		
		AmazonS3 s3mock = createMock(AmazonS3.class);
		expect(s3mock.listObjects(BUCKET, "a/b/c")).andReturn(listing1);
		expect(s3mock.listNextBatchOfObjects(listing1)).andReturn(listing2);
		expect(s3mock.listNextBatchOfObjects(listing2)).andReturn(listing3);
		replayAll();
		
		String[] actualKeys = StreamSupport.stream(new S3ObjectSpliterator(s3mock, BUCKET, "a/b/c"), false)
			.map(S3ObjectSummary::getKey)
			.collect(Collectors.toList())
			.toArray(new String[0]);
		assertArrayEquals(keys, actualKeys); 
}

}
