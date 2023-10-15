package com.demat.invoice.aws.service;

import static org.slf4j.LoggerFactory.getLogger;

import com.demat.invoice.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;


@Service(S3ResolverService.SERVICE_NAME)
public class S3ResolverServiceImpl implements S3ResolverService {

  private static final Logger log = getLogger(S3ResolverServiceImpl.class);

  public S3Service initS3Service() {
    S3Service resolvedS3Service = null;
    try {
        resolvedS3Service = SpringContextHelper.getBean(S3Service.class, AmazonS3ServiceImpl.SERVICE_NAME);
    }
    catch (NoSuchBeanDefinitionException e) {
      log.error("Could not resolve an S3Service", e);
      resolvedS3Service = null;
    }
    return resolvedS3Service;
  }

}
