package com.demat.invoice.aws.utils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory.Builder;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3control.*;
import com.amazonaws.services.s3control.model.*;
import com.amazonaws.services.securitytoken.*;
import org.apache.commons.lang3.StringUtils;

public class AwsHelper {

  public static Builder getLambdaClient(String accessKey, String secretKey, String region, String functionName) {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    Builder lambdaClient = LambdaInvokerFactory.builder()
        .lambdaClient(AWSLambdaClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion(Regions.valueOf(region))
            .build())
        .lambdaFunctionNameResolver((method, annotation, config) -> functionName);
    return lambdaClient;
  }

  public static AmazonS3 getAmazonS3Client(String accessKey, String secretKey, String region) {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    return AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withRegion(Regions.valueOf(region))
        .build();
  }

  /* AmazonS3Client used for S3 Compatible Service */
  public static AmazonS3 getAmazonS3ClientWithEndpoint(String accessKey, String secretKey, String region, String serviceEndpoint) {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    return AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
        .build();
  }

  public static AWSS3Control getAmazonS3ControlClient(String accessKey, String secretKey, String region, String accountId) {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    return AWSS3ControlClient.builder()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withRegion(Regions.valueOf(region))
        .build();
  }

  public static AWSSecurityTokenService getAmazonSecurityTokenService(String accessKey, String secretKey, String region) {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    return AWSSecurityTokenServiceClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withRegion(Regions.valueOf(region))
        .build();
  }

  public static String createRoleArn(String accountId, String roleName) {
    return "arn:aws:iam::" + accountId + ":role/" + roleName;
  }

  public static String createSnsTopicArn(String accountId, String region, String topicName) {
    return "arn:aws:sns:" + region + ":" + accountId + ":" + topicName;
  }

  public static String getTopicNameFromArn(String topicArn) {
    return StringUtils.substringAfterLast(topicArn, ":");
  }

  public static String createS3Arn(String bucket) {
    return "arn:aws:s3:::" + bucket;
  }

  private static String removeEndingSlash(String targetKeyPrefix) {
    return targetKeyPrefix != null && !targetKeyPrefix.isEmpty() && targetKeyPrefix.endsWith("/")
      ? targetKeyPrefix.substring(0, targetKeyPrefix.length() - 1)
      : targetKeyPrefix;
  }

  public static JobOperation createJobOperation(String batchOperationTargetBucket, String jobOperationPrefix) {
    JobOperation jobOperation = new JobOperation()
        .withS3PutObjectCopy(new S3CopyObjectOperation()
            .withTargetResource(createS3Arn(batchOperationTargetBucket))
            .withTargetKeyPrefix(removeEndingSlash(jobOperationPrefix))
        );
    return jobOperation;
  }

  public static JobManifest createJobManifest(String manifestBucket, String manifestKey, String eTag, JobManifestFormat jobManifestFormat) {
    if (StringUtils.isEmpty(eTag)) {
      throw new AmazonServiceException("Failed to upload manifest file");
    }
    JobManifest manifest = new JobManifest()
        .withSpec(new JobManifestSpec()
            .withFormat(jobManifestFormat)
            .withFields(new String[] {
                "Bucket", "Key"
            }))
        .withLocation(new JobManifestLocation()
            .withObjectArn(createS3Arn(manifestBucket) + "/" + manifestKey)
            .withETag(eTag));
    return manifest;
  }

  public static JobReport createJobReport(String targetBucket, String jobReportPrefix, JobReportFormat jobReportFormat, JobReportScope jobReportScope) {
    JobReport jobReport = new JobReport()
        .withBucket(createS3Arn(targetBucket))
        .withPrefix(jobReportPrefix)
        .withFormat(jobReportFormat)
        .withEnabled(true)
        .withReportScope(jobReportScope);
    return jobReport;
  }
}
