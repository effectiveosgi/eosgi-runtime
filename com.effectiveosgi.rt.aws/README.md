Effective OSGi RT: AWS Library and Component
============================================

## API

 * `com.effectiveosgi.rt.aws.S3ObjectSpliterator`: can be used to wrap an S3 directory listing as a Java 8 Stream.

## Services

 * `com.amazonaws.services.s3.AmazonS3` service is published based on a (factory) configuration PID `com.effectiveosgi.rt.aws.s3`. Refer to the bundle's metatype definition.

## Gogo Commands

 * `s3:ls <bucket> [<prefix>]`: list objects in an S3 bucket
