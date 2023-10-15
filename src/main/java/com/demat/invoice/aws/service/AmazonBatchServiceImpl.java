/**
 *
 */
package com.demat.invoice.aws.service;

import com.amazonaws.services.lambda.invoke.LambdaFunction;
import com.demat.invoice.aws.lambda.BatchJobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Map;

import static com.demat.invoice.aws.utils.AwsHelper.getLambdaClient;
import static java.lang.System.currentTimeMillis;

/**
 * @author Doru <tciolan.externe@generixgroup.com>
 */
public class AmazonBatchServiceImpl implements AmazonBatchService {

  private static final Logger log = LoggerFactory.getLogger(AmazonBatchServiceImpl.class);

  public interface AmazonLambdaKpiService {
    @LambdaFunction
    BatchJobResponse launchBatchJob(Map<String, Object> input);

    @LambdaFunction
    Boolean isBatchJobAvailable();
  }

  // General configuration
  @Value("${aws.core.enabled:}")
  private boolean enabled;

  @Value("${aws.core.accesskey:}")
  private String accessKey;

  @Value("${aws.core.secretkey:}")
  private String secretKey;

  @Value("${aws.core.region:}")
  private String region;

  @Value("${aws.kpi.batch.lambda.launch:}")
  private String batchJobFunctionName;

  @Value("${aws.kpi.batch.lambda.check:}")
  private String checkBatchJobFunctionName;

  @Override
  @PostConstruct
  public void init() {
    long start = currentTimeMillis();
    log.info("START AMAZON BATCH SERVICES INITIALIZATION...");
    if (!enabled) {
      log.info("\t Service status : disabled");
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
      Assert.hasText(batchJobFunctionName, "Launch name is required");
      Assert.hasText(checkBatchJobFunctionName, "Check name is required");
      log.info("\t Region : {}", region);
    }
    catch (Exception e) {
      log.error(e.getMessage());
      log.error("AMAZON BATCH SERVICES NOT STARTED in {}ms.", currentTimeMillis() - start);
    }
  }

  @Override
  public BatchJobResponse launchKpiJob(Map<String, Object> parameters) {
    return getLambdaClient(accessKey, secretKey, region, batchJobFunctionName)
        .build(AmazonLambdaKpiService.class)
        .launchBatchJob(parameters);
  }

  @Override
  public boolean isKpiJobAvailable() {
    try {
      return getLambdaClient(accessKey, secretKey, region, checkBatchJobFunctionName)
          .build(AmazonLambdaKpiService.class)
          .isBatchJobAvailable();
    }
    catch (Exception exception) {
      log.error(String.format("There was an error executing %s : %s", checkBatchJobFunctionName, exception.getMessage()), exception);
      return false;
    }
  }
}
