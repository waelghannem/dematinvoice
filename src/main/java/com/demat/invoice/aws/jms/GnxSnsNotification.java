package com.demat.invoice.aws.jms;

import com.amazonaws.services.sns.message.SnsNotification;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public abstract class GnxSnsNotification<T> {

  @SerializedName("MessageId")
  private String messageId;
  @SerializedName("TopicArn")
  private String topicArn;
  @SerializedName("Timestamp")
  private Date timestamp;
  @SerializedName("Subject")
  private String subject;
  @SerializedName("Message")
  private String mesage;
  private T notificationMessage;
  @SerializedName("Type")
  private String type;

  private SnsNotification snsNotification;

  public GnxSnsNotification(SnsNotification snsNotification) {
    this.messageId = snsNotification.getMessageId();
    this.topicArn = snsNotification.getTopicArn();
    this.timestamp = snsNotification.getTimestamp();
    this.subject = snsNotification.getSubject();
    this.notificationMessage = createNotification(snsNotification);
    this.snsNotification = snsNotification;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getTopicArn() {
    return topicArn;
  }

  public void setTopicArn(String topicArn) {
    this.topicArn = topicArn;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMesage() {
    return mesage;
  }

  public void setMesage(String mesage) {
    this.mesage = mesage;
  }

  public T getNotificationMessage() {
    return notificationMessage;
  }

  public void setNotificationMessage(T notificationMessage) {
    this.notificationMessage = notificationMessage;
  }

  public void unsubscribeFromTopic() {
    if (snsNotification != null) {
      snsNotification.unsubscribeFromTopic();
    }
  }

  protected abstract T createNotification(SnsNotification snsNotification);

}
