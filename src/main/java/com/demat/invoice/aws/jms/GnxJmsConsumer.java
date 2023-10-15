package com.demat.invoice.aws.jms;

import org.springframework.messaging.Message;

import java.util.List;

public interface GnxJmsConsumer {

  public List<String> getSubjectNames();

  public void handleNotification(final Message<String> message);

}
