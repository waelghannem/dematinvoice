package com.demat.invoice.aws.service;

public interface S3ResolverService {

  final String SERVICE_NAME = "s3ServiceResolverService";

  public S3Service initS3Service();

}
