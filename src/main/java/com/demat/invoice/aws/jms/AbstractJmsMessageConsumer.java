package com.demat.invoice.aws.jms;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.demat.invoice.utils.GsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

public abstract class AbstractJmsMessageConsumer {

  protected static final Logger log = LoggerFactory.getLogger(AbstractJmsMessageConsumer.class);
  private String latestMessageId;

  protected <T extends GnxSnsNotification<M>, M> T processMessage(final Message<String> message,
      Class<T> type, Class<M> messageType) {
    T snsNotification = createSnsNotification(message.getPayload(), type, messageType);
    if (snsNotification == null) {
      log.debug("Dismissed malformed message");
      return null;
    }
    if (StringUtils.equals(latestMessageId, snsNotification.getMessageId())) {
      log.debug("Dismissed message (" + snsNotification.getSubject() + ") id:" + latestMessageId);
      return null;
    }
    latestMessageId = snsNotification.getMessageId();
    return snsNotification;
  }

  protected static <T extends GnxSnsNotification<M>, M> T createSnsNotification(String snsNotification, Class<T> type,
      Class<M> messageType) {
    Gson gson = GsonHelper.getGson();
    T notif = null;
    try {
      notif = gson.fromJson(snsNotification, type);
    }
    catch (JsonSyntaxException ex) {
      log.error("An error occured while reading the SnsNotification. Cannot read snsNotification format");
      return null;
    }

    try {
      notif.setNotificationMessage(gson.fromJson(notif.getMesage(), messageType));
      return notif;
    }
    catch (JsonSyntaxException ex) {
      log.error("An error occured while reading the SnsNotification. Malformed body message: {}", notif.getMesage());
      return null;
    }
  }

}
