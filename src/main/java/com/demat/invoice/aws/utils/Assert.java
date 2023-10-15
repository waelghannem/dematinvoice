package com.demat.invoice.aws.utils;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Assert methods with localization based on {@link org.springframework.util.Assert}
 *
 * @author Lyna FUNG
 * @company Byzaneo
 * @date 2 mai 2014
 */
public abstract class Assert extends org.springframework.util.Assert { // NOSONAR : class names shadow interfaces or superclasses

  /**
   * Assert a boolean expression, throwing {@code IllegalArgumentException} if the test result is {@code false}.
   *
   * <pre class="code">
   * Assert.isTrue(i &gt; 0, "The value must be greater than zero");
   * </pre>
   *
   * @param expression a boolean expression
   * @param messageKey the exception messageKey to use if the assertion fails. It should be like basename.msg_key form
   * @throws IllegalArgumentException if expression is {@code false}
   */
  public static void isTrue(boolean expression, String messageKey, String defaultMessage, Object... args) {
    if (!expression) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that an object is {@code null} .
   *
   * <pre class="code">
   * Assert.isNull(value, "The value must be null");
   * </pre>
   *
   * @param object the object to check
   * @throws IllegalArgumentException if the object is not {@code null}
   */
  public static void isNull(Object object, String messageKey, String defaultMessage, Object... args) {
    if (object != null) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that an object is not {@code null} .
   *
   * <pre class="code">
   * Assert.notNull(clazz, "The class must not be null");
   * </pre>
   *
   * @param object the object to check
   * @throws IllegalArgumentException if the object is {@code null}
   */
  public static void notNull(Object object, String messageKey, String defaultMessage, Object... args) {
    if (object == null) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that the given String is not empty; that is, it must not be {@code null} and not the empty String.
   *
   * <pre class="code">
   * Assert.hasLength(name, "Name must not be empty");
   * </pre>
   *
   * @param text the String to check
   * @see StringUtils#hasLength
   */
  public static void hasLength(String text, String messageKey, String defaultMessage, Object... args) {
    if (!StringUtils.hasLength(text)) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that the given String has valid text content; that is, it must not be {@code null} and must contain at least one non-whitespace
   * character.
   *
   * <pre class="code">
   * Assert.hasText(name, "'name' must not be empty");
   * </pre>
   *
   * @param text the String to check
   * @see StringUtils#hasText
   */
  public static void hasText(String text, String messageKey, String defaultMessage, Object... args) {
    if (!StringUtils.hasText(text)) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that the given text does not contain the given substring.
   *
   * <pre class="code">
   * Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");
   * </pre>
   *
   * @param textToSearch the text to search
   * @param substring the substring to find within the text
   */
  public static void doesNotContain(String textToSearch, String substring, String messageKey, String defaultMessage, Object... args) {
    if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
        textToSearch.contains(substring)) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that an array has elements; that is, it must not be {@code null} and must have at least one element.
   *
   * <pre class="code">
   * Assert.notEmpty(array, "The array must have elements");
   * </pre>
   *
   * @param array the array to check
   * @throws IllegalArgumentException if the object array is {@code null} or has no elements
   */
  public static void notEmpty(Object[] array, String messageKey, String defaultMessage, Object... args) {
    if (ObjectUtils.isEmpty(array)) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that an array has no null elements. Note: Does not complain if the array is empty!
   *
   * <pre class="code">
   * Assert.noNullElements(array, "The array must have non-null elements");
   * </pre>
   *
   * @param array the array to check
   * @throws IllegalArgumentException if the object array contains a {@code null} element
   */
  public static void noNullElements(Object[] array, String messageKey, String defaultMessage, Object... args) {
    if (array != null) {
      for (Object element : array) {
        if (element == null) {
          throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
        }
      }
    }
  }

  /**
   * Assert that a collection has elements; that is, it must not be {@code null} and must have at least one element.
   *
   * <pre class="code">
   * Assert.notEmpty(collection, "Collection must have elements");
   * </pre>
   *
   * @param collection the collection to check
   * @throws IllegalArgumentException if the collection is {@code null} or has no elements
   */
  public static void notEmpty(Collection<?> collection, String messageKey, String defaultMessage, Object... args) {
    if (CollectionUtils.isEmpty(collection)) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert that a Map has entries; that is, it must not be {@code null} and must have at least one entry.
   *
   * <pre class="code">
   * Assert.notEmpty(map, "Map must have entries");
   * </pre>
   *
   * @param map the map to check
   * @throws IllegalArgumentException if the map is {@code null} or has no entries
   */
  public static void notEmpty(Map<?, ?> map, String messageKey, String defaultMessage, Object... args) {
    if (CollectionUtils.isEmpty(map)) {
      throw new IllegalArgumentException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

  /**
   * Assert a boolean expression, throwing {@code IllegalStateException} if the test result is {@code false}. Call isTrue if you wish to
   * throw IllegalArgumentException on an assertion failure.
   *
   * <pre class="code">
   * Assert.state(id == null, "The id property must not already be initialized");
   * </pre>
   *
   * @param expression a boolean expression
   * @throws IllegalStateException if expression is {@code false}
   */
  public static void state(boolean expression, String messageKey, String defaultMessage, Object... args) {
    if (!expression) {
      throw new IllegalStateException(getMessage(messageKey, defaultMessage, null, args));
    }
  }

    public static final String getMessage(final String messageKey, final String defaultMessage, final Locale locale, final Object... args) {
      return defaultMessage;
    }

}
