/**
 *
 */
package com.demat.invoice.aws.utils;

import com.amazonaws.*;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.copy;

/**
 * @author faboulaye
 */
public class AmazonArchivingHelper {

  private static final Logger log = LoggerFactory.getLogger(AmazonArchivingHelper.class);

  public static void waitForCompletion(Transfer xfer) {
    try {
      xfer.waitForCompletion();
    }
    catch (AmazonServiceException e) {
      log.error("Archiving service error", e);
    }
    catch (AmazonClientException e) {
      log.error("Archiving client error", e);
    }
    catch (InterruptedException e) {
      log.error("Transfer interrupted", e);
    }
  }

  public static UploadResult waitForUploadResult(Transfer xfer) {
    UploadResult uploadResult = null;
    try {
      uploadResult = ((Upload) xfer).waitForUploadResult();
    }
    catch (AmazonS3Exception e)
    {
      throw new UnhandledCharacterException("unhandled character");
    }
    catch (AmazonServiceException e) {
      log.error("Archiving service error", e);
    }
    catch (AmazonClientException e) {
      log.error("Archiving client error", e);
    }
    catch (InterruptedException e) {
      log.error("Transfer interrupted", e);
    }
    return uploadResult;
  }

  /**
   * Prints basic transfer information.
   *
   * @param xfer
   */
  public static void showTransferDetails(Transfer xfer) {
    log.debug("Transfer description : {}", xfer.getDescription());
    log.debug("Transfering {} bytes", xfer.getProgress()
        .getTotalBytesToTransfer());
    log.debug("Transfer state : {}", xfer.getState());
  }

  /**
   * Prints progress of a multiple file upload while waiting for it to finish.
   *
   * @param multiUpload
   */
  public static void showMultiTransferProgress(MultipleFileUpload multiUpload) {
    log.debug("Transfer description : {}", multiUpload.getDescription());

    Collection<? extends Upload> subXfers = new ArrayList<Upload>();
    subXfers = multiUpload.getSubTransfers();

    do {
      log.debug("Subtransfer progress");
      for (Upload u : subXfers) {
        log.debug("Upload description {}", u.getDescription());
        if (u.isDone()) {
          TransferState xferState = u.getState();
          log.debug("Upload state {}", xferState);
        }
        else {
          TransferProgress progress = u.getProgress();
          log.debug("Transfer progress : {}/{} ({}%)", progress.getBytesTransferred(),
              progress.getTotalBytesToTransfer(), progress.getPercentTransferred());
        }
      }
    }
    while (!multiUpload.isDone());
    log.debug("Transfer state : {}", multiUpload.getState());
  }

  public static String buildKeyWithUserAndEnvironment(String owner, String environment, LocalDate date, String filename) {
    Assert.hasText(owner, "Owner is required");
    Assert.hasText(environment, "Environment is required");
    return buildKeyWithDirection(owner, environment, date, filename);
  }

  public static String buildKeyWithDirection(String docFromOrTo, String direction, LocalDate date, String filename) {
    Assert.hasText(docFromOrTo, "From or To is required");
    Assert.hasText(direction, "Direction is required");
    Assert.notNull(date, "Date is required");
    Assert.hasText(filename, "Filename is required");

    return String.join("/", docFromOrTo, direction, String.valueOf(date.getYear()),
        date.format(DateTimeFormatter.ofPattern("MM")),
        date.format(DateTimeFormatter.ofPattern("dd")), filename);
  }

  public static String buildKeyForTemporaryFolder(String prefix, LocalDate date, String filename) {
    Assert.hasText(prefix, "Prefix is required");
    Assert.notNull(date, "Date is required");
    Assert.hasText(filename, "Filename is required");
    return String.join("/", prefix, String.valueOf(date.getYear()), String.valueOf(date.getMonthValue()),
        String.valueOf(date.getDayOfMonth()), filename);
  }

  public static void populateMetadata(ObjectMetadata metadata, Map<String, String> arguments) {
    Assert.notNull(metadata, "object metadata is required");
    Assert.notNull(arguments, "Arguments are required");
    arguments.entrySet()
        .forEach(e -> metadata.addUserMetadata(e.getKey(), e.getValue()));
  }

  public static void populateMetadata(ObjectMetadata metadata, Map<String, String> arguments, File file) {
    Assert.notNull(file, "file is required");
    populateMetadata(metadata, arguments);
    metadata.setContentLength(file.length());
    if (StringUtils.isEmpty(metadata.getContentType())) {
      metadata.setContentType(Mimetypes.getInstance()
          .getMimetype(file));
    }
  }

  public static String replaceDomainOfUrl(String url, String newDomain) {
    if (StringUtils.isEmpty(url)) {
      return StringUtils.EMPTY;
    }

    if (StringUtils.isEmpty(newDomain)) {
      return url;
    }
    return url.replaceFirst("https:\\/\\/(\\w|\\.|-)*\\.amazonaws.com", newDomain);
  }

  public static String generateAlphanumeric(int numchars) {
    return RandomStringUtils.randomAlphanumeric(numchars);
  }

  public static Path unzipArchiveZip(Path archiveZip) throws IOException {
    Path unzipFolder = Paths.get(archiveZip.getParent()
        .toString(), FilenameUtils.removeExtension(archiveZip.toFile()
            .getName()));
    try (ZipInputStream zipStream = new ZipInputStream(new FileInputStream(archiveZip.toFile()))) {
      unzip(zipStream, unzipFolder);
    }
    catch (Exception e) {
      log.error("Failed to unzip archive", e);
    }
    finally {
      Files.deleteIfExists(archiveZip);
    }
    return unzipFolder;
  }

  public static void unzip(final ZipInputStream zipStream, Path destination) throws IOException {
    for (ZipEntry entry; (entry = zipStream.getNextEntry()) != null; zipStream.closeEntry()) {
      // unzip entry
      String entryName = entry.getName();
      Path target = destination.resolve(entryName);
      if (entry.isDirectory()) {
        Files.createDirectories(target);
      }
      else {
        Path parent = target.getParent();
        if (!Files.exists(parent)) {
          Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(target)) {
          copy(zipStream, out);
        }
      }
    }
  }
}
