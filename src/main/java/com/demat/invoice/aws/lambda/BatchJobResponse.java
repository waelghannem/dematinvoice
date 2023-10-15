package com.demat.invoice.aws.lambda;

import java.io.Serializable;

public class BatchJobResponse implements Serializable {

  private String jobId;
  private String error;

  public BatchJobResponse() {
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

}
