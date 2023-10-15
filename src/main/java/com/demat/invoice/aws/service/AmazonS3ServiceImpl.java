/**
 *
 */
package com.demat.invoice.aws.service;

import com.amazonaws.services.s3control.AWSS3Control;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

import static com.demat.invoice.aws.utils.AwsHelper.*;
import static java.lang.System.currentTimeMillis;

/**
 * @author faboulaye
 */

//@Service(AmazonS3ServiceImpl.SERVICE_NAME)
public class AmazonS3ServiceImpl extends AbstractS3Service {

  public static final String SERVICE_NAME = "gnxAmazonS3Service";

  // General configuration
  @Value("${aws.core.enabled:}")
  private boolean enabled;

  @Value("${aws.core.accesskey:}")
  private String accessKey;

  @Value("${aws.core.secretkey:}")
  private String secretKey;

  @Value("${aws.core.region:}")
  private String region;

  @Value("${aws.archiving.s3.ssealgorithm:}")
  private String sseAlgorithm;

  @Value("${aws.archiving.s3.objectlock:false}")
  private boolean objectLock;

  // Configuration spécifique au service d'archivage
  @Value("${aws.archiving.s3.bucket:}")
  private String bucket;

  private boolean isS3ArchiveServiceAvailable = false;

  private AWSS3Control s3ControlClient;

  private AWSSecurityTokenService awsSecurity;

  @Override
  @PostConstruct
  public void init() {
    long start = currentTimeMillis();
    log.info("AMAZON ARCHIVING SERVICES INITIALIZATION...");
    if (!enabled) {
      log.info("\t Service status : disabled");
      log.warn("AMAZON ARCHIVING SERVICES NOT STARTED in {}ms.", currentTimeMillis() - start);
      return;
    }
    else {
      log.info("\t Service status : enabled");
    }

    try {
      // Checking general configuration
      Assert.hasText(accessKey, "Access key is required");
      Assert.hasText(secretKey, "Secret key is required");
      Assert.hasText(region, "Region is required");
      Assert.hasText(sseAlgorithm, "SSL algorithm is required");
      log.info("\t Region : {}", region);
      this.awsS3Client = getAmazonS3Client(accessKey, secretKey, region);
      this.awsSecurity = getAmazonSecurityTokenService(accessKey, secretKey, region);
      String accountId = awsSecurity.getCallerIdentity(new GetCallerIdentityRequest())
          .getAccount();
      this.s3ControlClient = getAmazonS3ControlClient(accessKey, secretKey, region, accountId);
    }
    catch (Exception e) {
      log.error(String.format("AMAZON ARCHIVING SERVICES NOT STARTED in %d ms.", currentTimeMillis() - start), e);
      return;
    }

    // checking S3 archive service configuration
    if (!bucket.isEmpty()) {
      try {
        Assert.hasText(bucket, "Bucket is required");
        log.info("\t Bucket's name : {}", bucket);
        checkIfBucketExist(this.bucket);
        this.isS3ArchiveServiceAvailable = true;
        log.info("AMAZON S3 ARCHIVING SERVICE STARTED in {}ms.", currentTimeMillis() - start);
      }
      catch (Exception e) {
        log.error(String.format("AMAZON ARCHIVING SERVICE NOT STARTED in %d ms.", currentTimeMillis() - start), e);
      }
    }
    else {
      log.warn("AMAZON ARCHIVING SERVICE INACTIVE");
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

  protected String getBucket() {
    return bucket;
  }

  protected AWSSecurityTokenService getAwsSecurity() {
    return awsSecurity;
  }

  @Override
  protected AWSS3Control getS3ControlClient() {
    return s3ControlClient;
  }

  @Override
  protected boolean isObjectLock() {
    return objectLock;
  }

  @Override
  public boolean isS3ArchiveServiceAvailable() {
    return this.isS3ArchiveServiceAvailable;
  }
}
