package com.demat.invoice.aws.jms;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.demat.invoice.utils.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JMS message consumer that will process messages from ${aws.core.sqs.queue} queue and will try to process them using the corresponding
 * GnxJmsConsumer for the topic
 *
 * @author Doru <tciolan.externe@generixgroup.com>
 */
@Component
@Conditional(JmsConfiguration.Condition.class)
public class JmsMessageConsumer {

  @Autowired
  private List<GnxJmsConsumer> gnxConsumers;

  protected static final Logger log = LoggerFactory.getLogger(JmsMessageConsumer.class);

  @JmsListener(destination = "${aws.core.sqs.queue:}", containerFactory = "jmsListenerContainerFactory")
  public void processMessage(@Payload final Message<String> message) throws RuntimeException {

    try {
      // this is in case the message is an SNS notification
      String subject = getElement(message, "Subject");
      gnxConsumers.stream()
          .filter(consumer -> consumer.getSubjectNames()
              .contains(subject))
          .findAny()
          .orElseThrow(() -> new UnknownSubjectException(subject))
          .handleNotification(message);

    }
    catch (UnknownSubjectException ex) {

      // TODO ********* test the JobStatusChangeEventRule subject and add it to the KPIservice subjectNames **********

      log.error("An error occured while reading the SnsNotification. Unknown message subject: " + ex.getSubject());
      log.error("Raw Message: " + message.getPayload());
    }
    catch (UnsupportedMessageException ex) {
      log.error("An error occured while reading the message. Unknown message format: " + message.getPayload());
    }

  }

  private String getElement(final Message<String> message, String key) throws UnsupportedMessageException {
    Gson gson = GsonHelper.getGson();
    return Optional.ofNullable(message)
        .map(Message::getPayload)
        .map(payload -> gson.fromJson(payload, JsonObject.class))
        .map(jsonObject -> jsonObject.get(key))
        .map(JsonElement::getAsString)
        .orElseThrow(() -> new UnsupportedMessageException());
  }

  private class UnsupportedMessageException extends Exception {
  }

  private class UnknownSubjectException extends Exception {

    private String subject;

    public UnknownSubjectException(String subject) {
      super();
      this.subject = subject;
    }

    public String getSubject() {
      return subject;
    }

  }
}
