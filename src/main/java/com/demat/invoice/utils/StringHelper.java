package com.demat.invoice.utils;

import com.mifmif.common.regex.Generex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Romain Rossi
 * @company Byzaneo
 * @version CVS $Revision: 1.2 $ $Date: 2008-05-29 13:06:12 $
 */
public final class StringHelper extends StringUtils {

  private static final Logger log = getLogger(StringHelper.class);

  /** The random number generator. */
  private static final Random RANDOM = new Random();

  private static final int MAX_GENERATION = 2000;
  /**
   * Set of characters that is valid. Must be printable, memorable, and "won't break HTML" (i.e., not ' <', '>', '&', '=', ...). or break
   * shell commands (i.e., not ' <', '>', '$', '!', ...). I, L and O are good to leave out, as are numeric zero and one.
   */
  private static final char[] SIMPLE_CHAR = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j',
      'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
      'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N',
      'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '2', '3',
      '4', '5', '6', '7', '8', '9' };

  private static final String CURRENT_REGEX = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@!%?*_,()#~\u0026\u00A3\u20AC])[A-Za-z0-9$@!%?*_,()#~\u0026\u00A3\u20AC]{8,32}";

  private static final String REGEX_USED_TO_GENERATE_PASSWORD = "[0-9]{2,8}[a-z]{2,8}[A-Z]{2,8}[$@!%?*_,()#~\u0026\u00A3\u20AC]{2,8}";

  /**
   * Generate a Password object with a random password.
   */
  public static final String generatePasswordLink(int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(SIMPLE_CHAR[RANDOM.nextInt(SIMPLE_CHAR.length)]);
    }
    return sb.toString();
  }

  public static final String generatePassword(String regex) {
    if (StringUtils.isBlank(regex)) {
      regex = CURRENT_REGEX;
    }
    Generex generex = new Generex(regex.equals(CURRENT_REGEX) ? REGEX_USED_TO_GENERATE_PASSWORD : removeLookarounds(regex));
    String randomPwd = null;

    for (int index = 0; index < MAX_GENERATION; index++) {
      randomPwd = generex.random();
      if (Pattern.matches(regex, randomPwd)) {
        return randomPwd;
      }
    }
    return null;
  }

  /**
   * Remove lookarounds of a regex expression
   *
   * @param regex
   * @return
   */
  public static final String removeLookarounds(String regex) {
    String[] groups = regex.split(
        "\\(\\?((?<=\\\\)[()\\[\\]]|\\[([^]]|(?<=[\\\\\\^])\\])*\\]|\\((?<arguments>(([^\\(\\)]*)|(\\([^\\(\\)]*\\))|(.*?))*)\\)|[^)])*\\)");
    StringBuilder generationRegex = new StringBuilder();
    for (String group : groups) {
      if (!group.startsWith("?")) {
        generationRegex.append(group);
      }
    }
    return generationRegex.toString();
  }

  /**
   * Special cases: . commaDelimitedToList(null) : [] . commaDelimitedToList(" ") : []
   *
   * @param str
   * @return
   */
  public static final List<String> commaDelimitedToList(final String str) {
    if (StringHelper.isBlank(str))
      return emptyList();

    return asList(split(str, ','));
  }

  /**
   * @param list
   * @return a list of converted Strings to Objects
   * @since {@link #toObject(String)}
   */
  public static final List<Object> toObjectList(final List<String> list) {
    if (list == null)
      return Collections.emptyList();

    List<Object> r = new ArrayList<>(list.size());
    for (String string : list)
      r.add(toObject(string));

    return r;
  }

  /**
   * @param list
   * @return an array of converted Strings to Objects
   * @since {@link #toObject(String)}
   */
  public static final Object[] toObjectArray(final List<String> list) {
    return list == null ? null : toObjectList(list).toArray();
  }

  /**
   * @param array
   * @return an array of converted Strings to Objects
   * @since {@link #toObject(String)}
   */
  public static final Object[] toObjectArray(final String[] array) {
    if (array == null)
      return new Object[0];

    Object[] r = new Object[array.length];
    for (int i = 0; i < array.length; i++)
      r[i] = toObject(array[i]);

    return r;
  }

  /**
   * @param map
   * @return a map with converted Strings values to Objects
   * @since {@link #toObject(String)}
   */
  public static final Map<String, Object> toObjectMap(final Map<String, String> map) {
    if (map == null)
      return null;

    Map<String, Object> r = new LinkedHashMap<>(map.size());
    for (Entry<String, String> e : map.entrySet())
      r.put(e.getKey(), toObject(e.getValue()));

    return r;
  }

  /**
   * @return converted {@link String} to an {@link Object} by guessing its {@link Type}. Supported guessed types:
   *         <ul>
   *         <li>Integer</li>
   *         <li>Boolean</li>
   *         <li><i>To be continued...</i></li>
   *         </ul>
   */
  public static final Object toObject(final String str) {
    if (StringHelper.isBlank(str))
      return str;

    String trimStr = str.trim();
    Object o = str;

    // Integer
    try {
      o = Integer.parseInt(trimStr);
    }
    catch (NumberFormatException ignored) {
      ;
    }

    // Boolean
    if (trimStr.equalsIgnoreCase(TRUE.toString()))
      o = TRUE;
    else if (trimStr.equalsIgnoreCase(FALSE.toString()))
      o = FALSE;

    // TODO continue...

    return o;
  }

  /**
   * @param str
   * @param c
   * @return array of the positions of given char c in the String str
   */
  public static final Integer[] indexesOf(final String str, final char c) {
    List<Integer> idxs = new ArrayList<Integer>();
    int curIdx = str.indexOf(c);
    while (curIdx != -1) {
      idxs.add(curIdx);
      curIdx = str.indexOf(c, curIdx + 1);
    }
    return idxs.toArray(new Integer[idxs.size()]);
  }

  /**
   * @param strings to concatenate
   * @return the concatenated list of strings skipping <code>null</code> values.
   */
  public static final String concat(final String... strings) {
    return concat(null, false, strings);
  }

  /**
   * @param separator (optional) value separator
   * @param trimValues TODO
   * @param strings to concatenate
   * @return the concatenated list of strings skipping <code>null</code> or empty values.
   */
  public static final String concat(final CharSequence separator, final boolean trimValues, final String... strings) {
    final StringBuilder buff = new StringBuilder();
    if (ArrayUtils.isNotEmpty(strings)) {
      for (String str : strings)
        if (isNotBlank(str))
          buff.append(buff.length() == 0 || separator == null ? "" : separator)
              .append(trimValues ? trim(str) : str);
    }
    return buff.toString();
  }

  /**
   * @param str
   * @return the HTML code of the given string (example: "[Euro Symbol]" = "&\#8364;")
   */
  public static final String toHtmlCode(final String str) {
    StringBuilder html = new StringBuilder();
    for (char c : str.toCharArray()) {
      html.append("&#")
          .append((int) c)
          .append(';');
    }
    String symbolAsHtml = html.toString();
    return symbolAsHtml;
  }

  /**
   * "best effort" attempt to get a plain text version from an original html input. In case of failure in the process, the original html is
   * returned.
   *
   * @param html
   * @return
   */
  public static String fromHtmlToText(String html) {
    if (html != null) {
      // NPE is handled, but we can avoid the throw
      try {
        // JSoup library could be used instead,
        // and could be more accurate, just use Jsoup.parse(html).text()
        return html.replaceAll("\\<.*?>", "");
      }
      catch (Exception e) {
        log.debug("Error getting fromHtlmToText() : {}", getRootCauseMessage(e));
        log.warn("html to plain conversion failed on input {} ({})", html, e.getMessage());
      }
    }
    return html;
  }

  /**
   * @param value String has to be processed
   * @return procesed value ex: value = name (2) will return name (3)
   */
  public static String incrementStringValue(List<String> existingNames, String value) {
    int maxIndex = existingNames.stream()
        .filter(p -> p.startsWith(value))
        .map(p -> p.replace(value, ""))
        .filter(p -> StringUtils.countMatches(p, "(") == 1 && StringUtils.countMatches(p, ")") == 1)
        .map(p -> p.replaceAll("[()]", ""))
        .map(p -> Integer.parseInt(p))
        .collect(Collectors.summarizingInt(Integer::intValue))
        .getMax();
    return maxIndex < 0 ? value + "(1)" : value + "(" + String.valueOf(maxIndex + 1) + ")";
  }
}
