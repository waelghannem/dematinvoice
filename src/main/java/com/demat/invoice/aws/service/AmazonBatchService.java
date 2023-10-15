/**
 *
 */
package com.demat.invoice.aws.service;

import com.demat.invoice.aws.lambda.BatchJobResponse;

import java.util.Map;

/**
 * @author Doru <tciolan.externe@generixgroup.com>
 */
public interface AmazonBatchService extends Initializable{

  final String SERVICE_NAME = "gnxAmazonBatchService";

  BatchJobResponse launchKpiJob(Map<String, Object> parameters);

  boolean isKpiJobAvailable();
}
