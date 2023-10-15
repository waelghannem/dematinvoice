package com.demat.invoice.aws.service;

import com.amazonaws.auth.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.quicksight.*;
import com.amazonaws.services.quicksight.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.demat.invoice.aws.utils.Assert;
import com.demat.invoice.aws.utils.AwsHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.System.currentTimeMillis;

@Service(AmazonQuickSightService.SERVICE_NAME)
public class AmazonQuickSightServiceImpl implements AmazonQuickSightService {

  private static final Logger log = LoggerFactory.getLogger(AmazonQuickSightServiceImpl.class);

  @Value("${aws.kpi.quicksight.enabled:false}")
  private boolean enabled;

  @Value("${aws.kpi.quicksight.accesskey:}")
  private String accessKey;

  @Value("${aws.kpi.quicksight.secretkey:}")
  private String secretKey;

  @Value("${aws.kpi.quicksight.region:}")
  private String region;

  @Value("${aws.kpi.quicksight.lifetime.cycle:}")
  private String lifeTimeCycle;

  @Value("${aws.kpi.quicksight.identity.type:}")
  private String identityType;

  private String awsAccountID;

  private AmazonQuickSight client;

  private boolean isQuickSightServiceAvailable;

  private AWSSecurityTokenService awsSecurity;

  @PostConstruct
  public void initClient() {

    log.info("START AMAZON QUICKSIGHT SERVICES INITIALIZATION...");
    long start = currentTimeMillis();
    if (!checkQuickSightParameters(start)) {
      return;
    }

    final AWSCredentialsProvider credsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAccessKey(), getSecretKey()));

    this.client = AmazonQuickSightClientBuilder.standard()
        .withRegion(Regions.valueOf(region))
        .withCredentials(credsProvider)
        .build();

    if (this.client == null) {
      log.error("AMAZON QUICKSIGHT CLIENT NOT STARTED in {}ms.", currentTimeMillis() - start);
      this.isQuickSightServiceAvailable = false;
      return;
    }

    try {

      this.awsSecurity = AwsHelper.getAmazonSecurityTokenService(accessKey, secretKey, region);
      this.awsAccountID = this.awsSecurity.getCallerIdentity(new GetCallerIdentityRequest())
          .getAccount();

      if (StringUtils.isEmpty(this.awsAccountID)) {

        log.error("User not found with this accesskey and secretKey, please contact your administrator");
        log.error("AMAZON QUICKSIGHT CLIENT NOT STARTED in {}ms.", currentTimeMillis() - start);
        this.isQuickSightServiceAvailable = false;
        return;

      }

      log.info("AMAZON QUICKSIGHT CLIENT STARTED in {}ms.", currentTimeMillis() - start);
      this.isQuickSightServiceAvailable = true;

    }
    catch (Exception e) {
      log.error("wrong configuration , please contact your administrator");
      log.error("AMAZON QUICKSIGHT CLIENT NOT STARTED in {}ms.", currentTimeMillis() - start);
      this.isQuickSightServiceAvailable = false;
      return;
    }

  }

  private boolean checkQuickSightParameters(long start) {
    if (!enabled) {
      log.info("\t Service status : disabled");
      log.error("AMAZON QUICKSIGHT CLIENT NOT STARTED in {}ms.", currentTimeMillis() - start);
      this.isQuickSightServiceAvailable = false;
      return false;
    }
    else {
      log.info("\t Service status : enabled");
    }

    try {
      Assert.hasText(accessKey, "Access key is required");
      Assert.hasText(secretKey, "Secret key is required");
      Assert.hasText(region, "Region is required");
      Assert.hasText(lifeTimeCycle, "Life time cycle is required");
      Assert.hasText(identityType, "Identity type is required");

      if (StringUtils.isEmpty(this.identityType)) {
        this.identityType = "IAM";
      }
      else {
        Assert.isTrue("IAM".equals(identityType) || "QUICKSIGHT".equals(identityType), "identityType should be IAM or QUICKSIGHT");
      }

      log.info("\t Region : {}", region);
      return true;
    }
    catch (Exception e) {
      log.error(e.getMessage());
      log.error("AMAZON QUICKSIGHT CLIENT NOT STARTED in {}ms.", currentTimeMillis() - start);
      this.isQuickSightServiceAvailable = false;
      return false;
    }
  }

  /**
   * <p>
   * The maximum number of results to be returned per request. Minimum 1 Maximum 100
   * </p>
   *
   * @return
   * @throws Exception
   */
  @Override
  public List<DashboardSummary> getAllDashBoardSummaries(Integer maxNumber) {
    ListDashboardsRequest listDashboardsRequest = createListDashboardsRequest(maxNumber);

    if (listDashboardsRequest == null || this.client == null) {
      return Collections.emptyList();
    }

    try {
      ListDashboardsResult listDashboardsResult = client.listDashboards(listDashboardsRequest);
      if (listDashboardsResult.getStatus() != 200) {
        return Collections.emptyList();
      }

      return listDashboardsResult.getDashboardSummaryList();
    }
    catch (Exception e) {
      log.error("An error has occurred getting dashboards. Please contact your administrator.", e);
      return Collections.emptyList();
    }
  }

  /**
   * <p>
   * Create the dashboard request
   * </p>
   *
   * @throws Exception
   */
  @Override
  public ListDashboardsRequest createListDashboardsRequest(Integer maxResult) {
    ListDashboardsRequest listDashboardsRequest = new ListDashboardsRequest();

    listDashboardsRequest.setMaxResults(maxResult);
    listDashboardsRequest.setAwsAccountId(this.awsAccountID);

    return listDashboardsRequest;
  }

  /**
   * <p>
   * Return all user's existing dashboard ID
   * </p>
   *
   * @throws Exception
   */
  @Override
  public List<String> getAllDashBoardIds(Integer maxNumber) {
    List<String> dashBoardIds = new ArrayList<>();

    getAllDashBoardSummaries(maxNumber).forEach(dashBoardSummary -> {
      dashBoardIds.add(dashBoardSummary.getDashboardId());
    });

    return dashBoardIds;
  }

  /**
   * <p>
   * Return the dashboard embeddable URL using default GET request parameters defined in properties file.
   * </p>
   *
   */
  @Override
  public GetDashboardEmbedUrlResult getDashboardEmbedUrlById(String dashBoardId) {
    return client.getDashboardEmbedUrl(createDashboardEmbedUrlRequest(dashBoardId));
  }

  /**
   * <p>
   * Return the dashboard embeddable URL, with default parameters
   * </p>
   *
   */
  private GetDashboardEmbedUrlRequest createDashboardEmbedUrlRequest(String dashBoardId) {
    return createDashboardEmbedUrlRequest(dashBoardId, Long.valueOf(this.lifeTimeCycle), this.identityType);
  }

  /**
   * <p>
   * Generates a server-side embeddable URL and authorization code
   * </p>
   *
   */
  private GetDashboardEmbedUrlRequest createDashboardEmbedUrlRequest(String dashBoardId, Long sessionLifeTime, String identityType) {
    GetDashboardEmbedUrlRequest getDashboardEmbedUrlRequest = new GetDashboardEmbedUrlRequest();

    getDashboardEmbedUrlRequest.setAwsAccountId(this.awsAccountID);
    getDashboardEmbedUrlRequest.setIdentityType(identityType);

    getDashboardEmbedUrlRequest.setSessionLifetimeInMinutes(sessionLifeTime);
    getDashboardEmbedUrlRequest.setDashboardId(dashBoardId);

    return getDashboardEmbedUrlRequest;
  }

  @Override
  public boolean isQuickSightServiceAvailable() {
    return this.isQuickSightServiceAvailable;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

}
