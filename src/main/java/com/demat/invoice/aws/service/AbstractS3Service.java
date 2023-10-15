package com.demat.invoice.aws.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.amazonaws.services.s3control.AWSS3Control;
import com.amazonaws.services.s3control.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.util.Md5Utils;
import com.demat.invoice.aws.utils.AmazonArchivingHelper;
import com.demat.invoice.aws.utils.AwsHelper;
import com.demat.invoice.aws.utils.UnhandledCharacterException;
import com.demat.invoice.utils.FileHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public abstract class AbstractS3Service implements S3Service {

  protected static final Logger log = LoggerFactory.getLogger(AbstractS3Service.class);

  protected AmazonS3 awsS3Client;

  @Override
  public abstract void init() throws Exception;

  @Override
  public void checkIfBucketExist(String name) {
    if (awsS3Client != null && !awsS3Client.doesBucketExistV2(name)) {
      throw new AmazonS3Exception("Failed to retreive bucket : " + name);
    }
  }

  @Override
  public void deleteBucketContents(String name) {
    try {
      ObjectListing objectListing = awsS3Client.listObjects(name);
      while (true) {
        for (Iterator<S3ObjectSummary> iterator = objectListing.getObjectSummaries()
            .iterator(); iterator.hasNext();) {
          S3ObjectSummary summary = iterator.next();
          awsS3Client.deleteObject(name, summary.getKey());
        }

        if (objectListing.isTruncated()) {
          objectListing = awsS3Client.listNextBatchOfObjects(objectListing);
        }
        else {
          break;
        }
      }
    }
    catch (AmazonServiceException e) {
      log.error("Failed to delete bucket", e);
    }
  }

  @Override
  public UploadResult uploadArchive(String key, File archive) {
    return uploadArchive(this.getBucket(), key, archive, null, null);
  }

  @Override
  public UploadResult uploadArchive(String bucket, String key, File archive) {
    return uploadArchive(bucket, key, archive, null, null);
  }

  @Override
  public UploadResult uploadArchive(String key, File archive, Map<String, String> metadatas) {
    return uploadArchive(this.getBucket(), key, archive, metadatas, null);
  }

  @Override
  public UploadResult uploadArchive(String key, File archive, Map<String, String> metadatas, List<Tag> tags) {
    return uploadArchive(this.getBucket(), key, archive, metadatas, tags);
  }

  @Override
  public UploadResult uploadArchive(String bucket, String key, File archive, Map<String, String> metadatas, List<Tag> tags) {
    TransferManager xferMgr = getTransferManager();
    UploadResult result = null;
    try (InputStream is = new FileInputStream(archive)) {

      ObjectMetadata metadata = new ObjectMetadata();
      //metadata.setSSEAlgorithm(getSseAlgorithm());
      metadata.setContentLength(FileHelper.sizeOf(archive));

      if (MapUtils.isNotEmpty(metadatas)) {
        AmazonArchivingHelper.populateMetadata(metadata, metadatas);
      }

      PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, "invoices", is, metadata);
      if (CollectionUtils.isNotEmpty(tags)) {
        putObjectRequest.setTagging(new ObjectTagging(tags));
      }

      if (isObjectLock()) {
        try {
          String contentMd5_b64 = Md5Utils.md5AsBase64(archive);
          metadata.setContentMD5(contentMd5_b64);
        }
        catch (Exception e) {
          throw new SdkClientException(
              "Unable to calculate MD5 hash: " + e.getMessage(), e);
        }
      }

      Upload xfer = xferMgr.upload(putObjectRequest);

      AmazonArchivingHelper.showTransferDetails(xfer);
      result = AmazonArchivingHelper.waitForUploadResult(xfer);

      if (xfer.getState() == TransferState.Failed || xfer.getState() == TransferState.Canceled) {
        log.error("Transfer state : {} ", xfer.getState());
        result = null;
      }
      else {
        log.debug("Transfer state : {} with algorithm encryption {}", xfer.getState(), metadata.getSSEAlgorithm());
      }
    }
    catch (UnhandledCharacterException e) {
      throw new UnhandledCharacterException(
          "unhandled character");
    }
    catch (AmazonServiceException | IOException e) {
      log.error(String.format("Failed to upload archive %s/%s", bucket, key), e);
    }
    finally {
      xferMgr.shutdownNow(false);
    }
    return result;
  }

  @Override
  public void uploadArchiveFolder(String key, File folder) {
    uploadArchiveFolder(getBucket(), key, folder, null);
  }

  @Override
  public void uploadArchiveFolder(String bucket, String key, File folder) {
    uploadArchiveFolder(bucket, key, folder, null);
  }

  @Override
  public void uploadArchiveFolder(String key, File folder, Map<String, String> metadatas) {
    uploadArchiveFolder(getBucket(), key, folder, metadatas);
  }

  @Override
  public void uploadArchiveFolder(String bucket, String key, File folder, Map<String, String> metadatas) {
    TransferManager xferMgr = getTransferManager();
    try {
      MultipleFileUpload multiUpload;
      if (MapUtils.isNotEmpty(metadatas)) {
        multiUpload = xferMgr.uploadDirectory(bucket, key, folder, true,
            (file, metadata) -> {
              AmazonArchivingHelper.populateMetadata(metadata, metadatas, file);
              //metadata.setSSEAlgorithm(getSseAlgorithm());
              metadata.setContentLength(FileHelper.sizeOf(file));
            });
      }
      else {
        multiUpload = xferMgr.uploadDirectory(bucket, key, folder, true);
      }
      AmazonArchivingHelper.showMultiTransferProgress(multiUpload);
      AmazonArchivingHelper.waitForCompletion(multiUpload);
    }
    catch (AmazonServiceException e) {
      log.error(String.format("Failed to upload folder %s/%s", bucket, key), e);
    }
    finally {
      xferMgr.shutdownNow(false);
    }
  }

  @Override
  public boolean downloadArchive(String key, String path) {
    return downloadArchive(getBucket(), key, path);
  }

  @Override
  public boolean downloadArchive(String bucket, String key, String path) {
    TransferManager xferMgr = getTransferManager();
    try {
      Download xfer = xferMgr.download(bucket, key, new File(path));
      AmazonArchivingHelper.showTransferDetails(xfer);
      AmazonArchivingHelper.waitForCompletion(xfer);
    }
    catch (AmazonServiceException e) {
      log.error(String.format("Failed to download archive %s/%s", bucket, key), e);
      return false;
    }
    xferMgr.shutdownNow(false);
    return true;
  }

  @Override
  public void downloadArchiveFolder(String key, String path) {
    downloadArchiveFolder(getBucket(), key, path);
  }

  @Override
  public void downloadArchiveFolder(String bucket, String key, String path) {
    TransferManager xferMgr = getTransferManager();
    try {
      MultipleFileDownload xfer = xferMgr.downloadDirectory(bucket, key, new File(path));
      AmazonArchivingHelper.showTransferDetails(xfer);
      AmazonArchivingHelper.waitForCompletion(xfer);
    }
    catch (AmazonServiceException e) {
      log.error(String.format("Failed to download folder %s/%s", bucket, key), e);
    }
    xferMgr.shutdownNow(false);
  }

  @Override
  public Optional<String> readArchive(String bucket, String key, Charset charset, String lineSeparator) {
    Objects.requireNonNull(charset);
    Objects.requireNonNull(lineSeparator);
    S3Object object = awsS3Client.getObject(new GetObjectRequest(bucket, key));
    if (object == null) { // Happens either if the object is absent or if the client is not allowed to read the object
      return Optional.empty();
    }
    StringBuilder str = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(object.getObjectContent(), charset))) {
      String line = reader.readLine();
      while (line != null) {
        str.append(line)
            .append(lineSeparator);
        line = reader.readLine();
      }
      return Optional.of(str.toString());
    }
    catch (IOException e) {
      log.error(String.format("Impossible to read S3 Object %s/%s", bucket, key), e);
      return Optional.empty(); // Makes the application robust, but the code does not know about the error
    }
  }

  @Override
  public boolean deleteArchive(String archiveKey) {
    return deleteArchive(getBucket(), archiveKey);
  }

  @Override
  public boolean deleteArchive(String bucket, String archiveKey) {
    try {
      awsS3Client.deleteObject(bucket, archiveKey);
    }
    catch (AmazonServiceException e) {
      log.error(String.format("Failed to delete archive %s/%s", bucket, archiveKey), e);
      return false;
    }
    return true;
  }

  @Override
  public void deleteArchiveFolder(String archiveKey) {
    deleteArchiveFolder(getBucket(), archiveKey);
  }

  @Override
  public void deleteArchiveFolder(String bucket, String archiveKey) {
    try {
      List<S3ObjectSummary> objects = awsS3Client.listObjects(bucket, archiveKey)
          .getObjectSummaries();
      if (CollectionUtils.isNotEmpty(objects)) {
        objects.forEach(obj -> deleteArchive(bucket, obj.getKey()));
      }
    }
    catch (AmazonServiceException e) {
      log.error(String.format("Failed to delete folder %s/%s", bucket, archiveKey), e);
    }
  }

  @Override
  public List<S3ObjectSummary> findAllArchives() {
    return findAllArchives(this.getBucket());
  }

  @Override
  public List<S3ObjectSummary> findAllArchives(String bucket) {
    ObjectListing ol = awsS3Client.listObjects(bucket);
    return ol.getObjectSummaries();
  }

  @Override
  public boolean hasArchive(String key) {
    return hasArchive(this.getBucket(), key);
  }

  @Override
  public boolean hasArchive(String bucket, String key) {
    return findAllArchives(bucket).stream()
        .filter(obj -> obj.getKey()
            .equals(key))
        .findAny()
        .isPresent();
  }

  protected TransferManager getTransferManager() {
    return TransferManagerBuilder.standard()
        .withS3Client(awsS3Client)
        .build();
  }

  protected AWSS3Control getS3ControlClient() {
    throw new UnsupportedOperationException("Retrieval of s3 control client not implemented.");
  }

  protected AWSSecurityTokenService getAwsSecurity() {
    throw new UnsupportedOperationException("Retrieval of aws security token service not implemented.");
  }

  protected abstract String getBucket();

  protected abstract String getSseAlgorithm();

  protected boolean isObjectLock() {
    return false;
  }

  private static String withEndingSlash(String s3ObjectPrefix) {
    return s3ObjectPrefix.endsWith("/") ? s3ObjectPrefix : s3ObjectPrefix + "/";
  }

  @Override
  public Optional<CreateJobResult> executeBatch(
      String batchOperationTargetBucket,
      String manifestKey, String manifestETag,
      String username, String batchOperationRole,
      String tempBucketTargetPrefix, String tempBucketReportPrefix) {

    String account = getAwsSecurity().getCallerIdentity(new GetCallerIdentityRequest())
        .getAccount();
    CreateJobResult result = getS3ControlClient().createJob(new CreateJobRequest()
        .withAccountId(account)
        .withOperation(AwsHelper.createJobOperation(
            batchOperationTargetBucket,
            withEndingSlash(tempBucketTargetPrefix) + username))
        .withManifest(AwsHelper.createJobManifest(batchOperationTargetBucket, manifestKey, manifestETag,
            JobManifestFormat.S3BatchOperations_CSV_20180820))
        .withReport(AwsHelper.createJobReport(
            batchOperationTargetBucket,
            withEndingSlash(tempBucketReportPrefix) + username,
            JobReportFormat.Report_CSV_20180820,
            JobReportScope.FailedTasksOnly))
        .withPriority(10)
        .withRoleArn(AwsHelper.createRoleArn(account, batchOperationRole))
        .withClientRequestToken(UUID.randomUUID()
            .toString())
        .withDescription("Copy Batch Operation")
        .withConfirmationRequired(false));
    log.info("Start batch operations with id {} successfully", result.getJobId());
    return Optional.ofNullable(result);
  }
}
