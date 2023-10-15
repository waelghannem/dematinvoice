/**
 *
 */
package com.demat.invoice.aws.service;

import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.amazonaws.services.s3control.model.CreateJobResult;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author faboulaye
 */
public interface S3Service extends Initializable {

  void checkIfBucketExist(String name);

  void deleteBucketContents(String name);

  UploadResult uploadArchive(String key, File archive);

  UploadResult uploadArchive(String bucket, String key, File archive);

  UploadResult uploadArchive(String key, File archive, Map<String, String> metadatas);

  UploadResult uploadArchive(String key, File archive, Map<String, String> metadatas, List<Tag> tags);

  UploadResult uploadArchive(String bucket, String key, File archive, Map<String, String> metadatas, List<Tag> tags);

  void uploadArchiveFolder(String key, File folder);

  void uploadArchiveFolder(String bucket, String key, File folder);

  boolean downloadArchive(String key, String path);

  /**
   * Downloads a S3 object as a local file.
   *
   * @param bucket The bucket of the source S3 object.
   * @param key The key of the source S3 object.
   * @param path The path of the destination local file.
   * @return {@code true} If the download is a success or {@code false} otherwise.
   */
  boolean downloadArchive(String bucket, String key, String path);

  void downloadArchiveFolder(String key, String path);

  void downloadArchiveFolder(String bucket, String key, String path);

  /**
   * Tries to read the content of a S3 object (or archive for consistency with the naming of this interface).
   *
   * @param bucket The S3 Bucket name.
   * @param key The S3 object key.
   * @param charset The object content's charset.
   * @param lineSeparator The object content's
   * @return The S3 object content as a string if possible. Or {@code null} if the object does not exists, if the application has not the
   *         read right on the object or if an error occured when reading the content.
   */
  Optional<String> readArchive(String bucket, String key, Charset charset, String lineSeparator);

  void uploadArchiveFolder(String key, File folder, Map<String, String> metadatas);

  void uploadArchiveFolder(String bucket, String key, File folder, Map<String, String> metadatas);

  boolean deleteArchive(String archiveKey);

  boolean deleteArchive(String bucket, String archiveKey);

  void deleteArchiveFolder(String archiveKey);

  void deleteArchiveFolder(String bucket, String archiveKey);

  List<S3ObjectSummary> findAllArchives();

  List<S3ObjectSummary> findAllArchives(String bucket);

  boolean hasArchive(String key);

  boolean hasArchive(String bucket, String key);

  boolean isS3ArchiveServiceAvailable();

  String getDefaultBucketName();

  public Optional<CreateJobResult> executeBatch(String targetBucket, String manifestKey, String manifestETag, String username,
      String batchOperationRole, String tempBucketTargetPrefix, String tempBucketReportPrefix);
}
