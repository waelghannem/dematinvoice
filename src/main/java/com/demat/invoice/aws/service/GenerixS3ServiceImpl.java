package com.demat.invoice.aws.service;

import com.amazonaws.SDKGlobalConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

import static com.demat.invoice.aws.utils.AwsHelper.getAmazonS3ClientWithEndpoint;
import static java.lang.System.currentTimeMillis;

@Service(GenerixS3ServiceImpl.SERVICE_NAME)
public class GenerixS3ServiceImpl extends AbstractS3Service {

  public static final String SERVICE_NAME = "gnxGenerixS3Service";

  @Value("${gnx.core.enabled:false}")
  private boolean enabled;

  @Value("${gnx.core.accesskey:}")
  private String accessKey;

  @Value("${gnx.core.secretkey:}")
  private String secretKey;

  @Value("${gnx.core.region:}")
  private String region;

  @Value("${gnx.archiving.s3.ssealgorithm:}")
  private String sseAlgorithm;

  @Value("${gnx.archiving.s3.bucket:}")
  private String bucket;

  @Value("${gnx.archiving.endpoint:}")
  private String endpoint;

  @Value("${gnx.archiving.s3.objectlock:false}")
  private boolean objectLock;

  private boolean isS3ArchiveServiceAvailable = false;

  @Override
  @PostConstruct
  public void init() throws Exception {
    long start = currentTimeMillis();
    log.info("GENERIX ARCHIVING SERVICES INITIALIZATION...");
    if (!enabled) {
      log.info("\t Generix S3 Service status : disabled");
      log.warn("GENERIX ARCHIVING SERVICES NOT STARTED in {} ms.", currentTimeMillis() - start);
      return;
    }
    else {
      log.info("\t Generix S3 Service status : enabled");
    }
    System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
    try {
      Assert.hasText(accessKey, "Access key is required");
      Assert.hasText(secretKey, "Secret key is required");
      Assert.hasText(region, "Region is required");
      Assert.hasText(sseAlgorithm, "SSL algorithm is required");
      Assert.hasText(endpoint, "Service endpoint is required");
      log.info("\t Region : {}", region);
      log.info("\t Endpoint : {}", endpoint);
      this.awsS3Client = getAmazonS3ClientWithEndpoint(accessKey, secretKey, region, endpoint);
    }
    catch (Exception e) {
      log.error(String.format("GENERIX S3 ARCHIVING SERVICES NOT STARTED in %d ms.", currentTimeMillis() - start), e);
      return;
    }
    if (!bucket.isEmpty()) {
      try {
        Assert.hasText(bucket, "Bucket is required");
        log.info("\t Bucket's name : {}", bucket);
        checkIfBucketExist(this.bucket);
        this.isS3ArchiveServiceAvailable = true;
        log.info("GENERIX S3 ARCHIVING SERVICE STARTED in {}ms.", currentTimeMillis() - start);
      }
      catch (Exception e) {
        log.error(String.format("GENERIX S3 ARCHIVING SERVICE NOT STARTED in %d ms.", currentTimeMillis() - start), e);
      }
    }
    else {
      log.warn("GENERIX S3 ARCHIVING SERVICE INACTIVE");
    }
  }

  @Override
  protected String getSseAlgorithm() {
    return sseAlgorithm;
  }

  @Override
  public String getDefaultBucketName() {
    return this.bucket;
  }

  @Override
  protected String getBucket() {
    return this.bucket;
  }

  @Override
  protected boolean isObjectLock() {
    return objectLock;
  }

  @Override
  public boolean isS3ArchiveServiceAvailable() {
    return isS3ArchiveServiceAvailable;
  }
}
