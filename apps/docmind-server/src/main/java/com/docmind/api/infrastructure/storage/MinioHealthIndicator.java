package com.docmind.api.infrastructure.storage;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.stereotype.Component;

@Component("minio")
@ConditionalOnEnabledHealthIndicator("minio")
public class MinioHealthIndicator extends AbstractHealthIndicator {

  private final MinioClient client;
  private final DocmindStorageProperties properties;

  public MinioHealthIndicator(MinioClient client, DocmindStorageProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    List<String> missingBuckets = new ArrayList<>();
    try {
      for (String bucket : properties.buckets().all()) {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
          missingBuckets.add(bucket);
        }
      }
    } catch (Exception exception) {
      builder.down().withDetail("reason", "object_storage_unavailable");
      return;
    }

    if (missingBuckets.isEmpty()) {
      builder.up().withDetail("bucket_count", properties.buckets().all().size());
    } else {
      builder.down().withDetail("reason", "required_bucket_missing");
    }
  }
}
