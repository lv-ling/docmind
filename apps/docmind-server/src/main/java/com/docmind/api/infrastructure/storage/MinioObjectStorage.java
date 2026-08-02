package com.docmind.api.infrastructure.storage;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.CopyObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.SourceObject;
import java.io.InputStream;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class MinioObjectStorage implements ObjectStorage {

  private final MinioClient client;

  public MinioObjectStorage(MinioClient client) {
    this.client = client;
  }

  @Override
  public String presignedPut(String bucket, String objectKey, Duration ttl) {
    try {
      return client.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Http.Method.PUT)
              .bucket(bucket)
              .object(objectKey)
              .expiry(Math.toIntExact(ttl.toSeconds()))
              .build());
    } catch (Exception exception) {
      throw new ObjectStorageException(exception);
    }
  }

  @Override
  public StoredObject stat(String bucket, String objectKey) {
    try {
      StatObjectResponse response =
          client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
      return new StoredObject(response.size(), response.etag(), response.contentType());
    } catch (Exception exception) {
      throw new ObjectStorageException(exception);
    }
  }

  @Override
  public InputStream open(String bucket, String objectKey) {
    try {
      return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception exception) {
      throw new ObjectStorageException(exception);
    }
  }

  @Override
  public String put(String bucket, String objectKey, byte[] content, String contentType) {
    try {
      return client
          .putObject(
              PutObjectArgs.builder()
                  .bucket(bucket)
                  .object(objectKey)
                  .contentType(contentType)
                  .stream(
                      new java.io.ByteArrayInputStream(content),
                      Long.valueOf(content.length),
                      Long.valueOf(-1L))
                  .build())
          .etag();
    } catch (Exception exception) {
      throw new ObjectStorageException(exception);
    }
  }

  @Override
  public String copyIfMatch(
      String sourceBucket,
      String sourceObjectKey,
      String sourceEtag,
      String destinationBucket,
      String destinationObjectKey) {
    try {
      return client.copyObject(
          CopyObjectArgs.builder()
              .bucket(destinationBucket)
              .object(destinationObjectKey)
              .source(
                  SourceObject.builder()
                      .bucket(sourceBucket)
                      .object(sourceObjectKey)
                      .matchETag(sourceEtag)
                      .build())
              .build())
          .etag();
    } catch (Exception exception) {
      throw new ObjectStorageException(exception);
    }
  }

  @Override
  public void delete(String bucket, String objectKey) {
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (Exception exception) {
      throw new ObjectStorageException(exception);
    }
  }
}
