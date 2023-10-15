package com.demat.invoice.utils;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.BeanFactory.FACTORY_BEAN_PREFIX;


@Component
public class SpringContextHelper implements ApplicationContextAware {
    private static final Logger log = getLogger(SpringContextHelper.class);

    /** Current application context */
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext; // NOSONAR static field updated by Spring application aware mechanism
        log.info("ApplicationContext set: {}", applicationContext);
    }

    public static final <T> T getBean(Class<T> beanClass) {
        return context == null ? null : context.getBean(beanClass);
    }

    public static final <T> Optional<T> getOptionalBean(Class<T> beanClass) {
        if (context == null || beanClass == null)
            return Optional.empty();
        try {
            return Optional.ofNullable(context.getBean(beanClass));
        }
        catch (BeansException be) {
            log.debug("Bean of type '{}' not found ({})", beanClass, be);
            return Optional.empty();
        }
    }

    public static final <T> Optional<T> getOptionalBean(Class<T> beanClass, String beanName) {
        if (context == null || beanClass == null || beanName == null)
            return Optional.empty();
        try {
            return Optional.ofNullable(context.getBean(beanName, beanClass));
        }
        catch (BeansException be) {
            log.debug("Bean of type '{}' with name '{}' not found ({})", beanClass, beanName, be);
            return Optional.empty();
        }
    }

    public static final <T> Map<String, T> getBeanOfType(Class<T> beanClass) {
        return context == null ? null : context.getBeansOfType(beanClass);
    }

    public static final Object getBean(final String beanId) {
        return context == null ? null : context.getBean(beanId);
    }
    public static final <T> T getBean(Class<T> type, String beanId) {
        return getBean(type, beanId, false);
    }

    public static final <T> T getBean(Class<T> type, String beanId, boolean optional) {
        try {
            return context.getBean(beanId, type);
        }
        catch (NullPointerException npe) {
            log.debug("Bean of type '{}' with identifier '{}' not found (context not yet initialized?): {}",
                type, beanId, npe);
            return null;
        }
        catch (ClassCastException | BeansException e) {
            if (optional)
                return null;
            throw e;
        }
    }

    public static final boolean exists(Class<?> type, String beanId) {
        return getBean(type, beanId, true) == null;
    }

    public static final Map<String, Object> getBeans(final ApplicationContext context, final Class<?>... excludedBeans) {
        if (context == null)
            return Collections.emptyMap();
        final Map<String, Object> beans = new HashMap<>();
        for (String name : context.getBeanDefinitionNames()) {
            if (isNotEmpty(excludedBeans)) {
                for (Class<?> excludedBean : excludedBeans) {
                    if (!contains(context.getBeanNamesForType(excludedBean), FACTORY_BEAN_PREFIX.concat(name)) // NOSONAR : function too deeply
                        && !contains(context.getBeanNamesForType(excludedBean), name)) {
                        beans.put(name, context.getBean(name));
                        break;
                    }
                }
            }
            else {
                beans.put(name, context.getBean(name));
            }
        }
        return beans;
    }

    public static final String getMessage(String messageKey) {
        return getMessage(messageKey, messageKey);
    }

    /**
     * Try to resolve the message. Return default message if no message was found.
     *
     * @param messageKey the code to lookup up, such as 'calculator.noRateSet'. Users of this class are encouraged to base message names on
     *          the relevant fully qualified class name, thus avoiding conflict and ensuring maximum clarity.
     * @param defaultMessage String to return if the lookup fails
     * @return the resolved message based on the default locale if the lookup was successful; otherwise the default message passed as a
     *         parameter
     * @see java.text.MessageFormat
     */
    public static final String getMessage(String messageKey, String defaultMessage) {
        return getMessage(messageKey, null, defaultMessage, Locale.getDefault());
    }

    /**
     * Try to resolve the message. Return default message if no message was found.
     *
     * @param messageKey the code to lookup up, such as 'calculator.noRateSet'. Users of this class are encouraged to base message names on
     *          the relevant fully qualified class name, thus avoiding conflict and ensuring maximum clarity.
     * @param args array of arguments that will be filled in for params within the message (params look like "{0}", "{1,date}", "{2,time}"
     *          within a message), or {@code null} if none.
     * @param defaultMessage String to return if the lookup fails
     * @return the resolved message based on the default locale if the lookup was successful; otherwise the default message passed as a
     *         parameter
     * @see java.text.MessageFormat
     */
    public static final String getMessage(String messageKey, Object[] args, String defaultMessage) {
        return getMessage(messageKey, args, defaultMessage, Locale.getDefault());
    }

    /**
     * Try to resolve the message. Return default message if no message was found.
     *
     * @param messageKey the code to lookup up, such as 'calculator.noRateSet'. Users of this class are encouraged to base message names on
     *          the relevant fully qualified class name, thus avoiding conflict and ensuring maximum clarity.
     * @param args array of arguments that will be filled in for params within the message (params look like "{0}", "{1,date}", "{2,time}"
     *          within a message), or {@code null} if none.
     * @param defaultMessage String to return if the lookup fails
     * @param locale the Locale in which to do the lookup
     * @return the resolved message if the lookup was successful; otherwise the default message passed as a parameter
     * @see java.text.MessageFormat
     */
    public static final String getMessage(String messageKey, Object[] args, String defaultMessage, Locale locale) {
        return context == null || messageKey == null
            ? defaultMessage
            : context.getMessage(messageKey, args, defaultMessage, locale);
    }

}
