package com.demat.invoice.aws.jms;

import com.demat.invoice.utils.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnxSnsHelper {

  private static final Logger log = LoggerFactory.getLogger(GnxSnsHelper.class);

  public static <T> T getSnsMessage(String message, Class<T> type) {
    try {
      return GsonHelper.getGson()
          .fromJson(message, type);
    }
    catch (Exception ex) {
      log.error("Cannot transform SNS message into format " + type);
    }
    return null;
  }
}
