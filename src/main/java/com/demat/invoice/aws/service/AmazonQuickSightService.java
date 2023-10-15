package com.demat.invoice.aws.service;

import com.amazonaws.services.quicksight.model.*;

import java.util.List;

public interface AmazonQuickSightService {

  final String SERVICE_NAME = "gnxAmazonQuickSightService";

  boolean isQuickSightServiceAvailable();

  List<DashboardSummary> getAllDashBoardSummaries(Integer maxNumber);

  List<String> getAllDashBoardIds(Integer maxNumber);

  ListDashboardsRequest createListDashboardsRequest(Integer maxResult);

  GetDashboardEmbedUrlResult getDashboardEmbedUrlById(String dashBoardId);

}
