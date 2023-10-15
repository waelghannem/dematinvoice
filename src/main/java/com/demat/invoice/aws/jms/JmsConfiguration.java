package com.demat.invoice.aws.jms;

import com.amazon.sqs.javamessaging.*;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.*;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import static com.demat.invoice.aws.utils.AwsHelper.getAmazonSecurityTokenService;

@Configuration
@EnableJms
public class JmsConfiguration {

    private static final int TIMEOUT_IN_SECONDS = 30;

    @Value("${aws.core.accesskey:}")
    private String accessKey;

    @Value("${aws.core.secretkey:}")
    private String secretKey;

    @Value("${aws.core.region:}")
    private String region;

    @Value("${aws.core.sqs.queue:}")
    private String sqsQueueName;

    private static final Logger log = LoggerFactory.getLogger(JmsConfiguration.class);

    @Bean(name = "jmsListenerContainerFactory")
    @Conditional(Condition.class)
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        return createJmsListenerContainerFactory(sqsQueueName);
    }

    private DefaultJmsListenerContainerFactory createJmsListenerContainerFactory(String sqsQueueName) {
        String accountId = getAmazonSecurityTokenService(accessKey, secretKey, region).getCallerIdentity(new GetCallerIdentityRequest())
            .getAccount();
        String sqsUrl = constructSqsUrl(accountId, sqsQueueName, region);
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);

        SqsClient sqsClient = SqsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();

        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), //
            sqsClient);
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver(new GnxDynamicDestinationResolver());
        factory.setConcurrency("1");
        factory.setReceiveTimeout(1000L * TIMEOUT_IN_SECONDS);
        factory.setSessionAcknowledgeMode(Integer.valueOf(Session.CLIENT_ACKNOWLEDGE));
        log.info("Listening on queue" + sqsQueueName + "> endppint: " + sqsUrl);
        return factory;
    }

    private String constructSqsUrl(String accountId, String sqsQueueName, String region) {
        return "https://sqs." + Regions.valueOf(region)
            .getName() + ".amazonaws.com/" + accountId + "/" + sqsQueueName;
    }

    public class GnxDynamicDestinationResolver extends DynamicDestinationResolver {
        @Override
        protected Queue resolveQueue(Session session, String queueName) throws JMSException {
            try {
                return super.resolveQueue(session, queueName);
            } catch (Exception e) {
                log.error("STOPPING KPI SERVICE... ");
                log.error("Could not resolve queue '" + queueName + "'");
                throw e;
            }
        }

    }

    public static class Condition implements ConfigurationCondition {

        /**
         * @see org.springframework.context.annotation.Condition#matches(ConditionContext,
         * AnnotatedTypeMetadata)
         */
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

            return Boolean.valueOf(context.getEnvironment()
                .getProperty("aws.core.enabled")) && StringUtils.isNotEmpty(context.getEnvironment()
                .getProperty("aws.core.sqs.queue"));
        }

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }
    }
}
