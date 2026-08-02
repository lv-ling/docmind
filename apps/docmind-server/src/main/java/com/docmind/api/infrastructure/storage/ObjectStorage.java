package com.docmind.api.infrastructure.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorage {

  String presignedPut(String bucket, String objectKey, Duration ttl);

  StoredObject stat(String bucket, String objectKey);

  InputStream open(String bucket, String objectKey);

  String put(String bucket, String objectKey, byte[] content, String contentType);

  String copyIfMatch(
      String sourceBucket,
      String sourceObjectKey,
      String sourceEtag,
      String destinationBucket,
      String destinationObjectKey);

  void delete(String bucket, String objectKey);

  record StoredObject(long size, String etag, String contentType) {}
}
