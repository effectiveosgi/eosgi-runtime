package com.effectiveosgi.rt.aws;

import java.util.List;
import java.util.ListIterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * <p>A spliterator that allows consuming an S3 directory listing with a Java 8 Stream. For example to list the keys of all non-empty entries:</p>
 * 
 * <pre>
 * AmazonS3 s3 = ...;
 * List&lt;String&gt; keys = StreamSupport.stream(new S3ObjectSpliterator(s3, bucketName, path), false)
 *     .filter(o -&gt; o.getSize() &gt; 0)
 *     .map(S3ObjectSummary::getKey)
 *     .collect(Collectors.toList());
 * </pre>
 * @author nbartlett
 *
 */
public class S3ObjectSpliterator extends AbstractSpliterator<S3ObjectSummary> {

	private final AmazonS3 s3;
	private final String bucket;
	private final String pathPrefix;
	
	private ObjectListing currentPage = null;
	private List<S3ObjectSummary> objects = null;
	int currentIndex = 0;

	/**
	 * Create an iterator.
	 * 
	 * @param s3
	 *            The AmazonS3 client connection.
	 * @param bucket
	 *            An S3 bucket name.
	 * @param pathPrefix
	 *            A path prefix: the spliterator returns paths beginning with
	 *            this prefix.
	 */
	public S3ObjectSpliterator(AmazonS3 s3, String bucket, String pathPrefix) {
		super(Long.MAX_VALUE, ORDERED | IMMUTABLE | NONNULL);
		this.s3 = s3;
		this.bucket = bucket;
		this.pathPrefix = pathPrefix;
	}
	
	@Override
	public void forEachRemaining(Consumer<? super S3ObjectSummary> action) {
		while (fetchData()) {
			for (ListIterator<S3ObjectSummary> iter = objects.listIterator(currentIndex); iter.hasNext(); ) {
				action.accept(iter.next());
				currentIndex++;
			}
		}
	}

	@Override
	public boolean tryAdvance(Consumer<? super S3ObjectSummary> action) {
		if (fetchData()) {
			action.accept(objects.get(currentIndex++));
			return true;
		}
		return false;
	}
	
	/**
	 * Fetch data from S3 if needed
	 * @return True if there is at least one unread record in the objects field.
	 */
	private boolean fetchData() {
		if (objects == null || currentIndex >= objects.size()) {
			// We are either before the first page or at the end of the current page.
			if (currentPage == null) {
				// Load the first page
				currentPage = s3.listObjects(bucket, pathPrefix);
			} else if (currentPage.isTruncated()) {
				// Load the next page
				currentPage = s3.listNextBatchOfObjects(currentPage);
			} else {
				return false;
			}
			objects = currentPage.getObjectSummaries();
			currentIndex = 0;
			return !objects.isEmpty();
		}
		return true;
	}

}
