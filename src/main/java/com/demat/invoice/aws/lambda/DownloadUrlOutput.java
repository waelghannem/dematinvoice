package com.demat.invoice.aws.lambda;

public class DownloadUrlOutput {

  private String downloadUrl;

  DownloadUrlOutput(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

}
