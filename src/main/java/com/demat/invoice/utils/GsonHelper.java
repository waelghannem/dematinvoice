package com.demat.invoice.utils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.demat.invoice.annotation.Exclude;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static java.text.DateFormat.DEFAULT;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author Romain Rossi <romain.rossi@byzaneo.com>
 * @company Byzaneo
 * @date 20 avr. 2010
 * @since 2.2 COM-60
 */
public class GsonHelper {
  private static final Logger log = getLogger(GsonHelper.class);

  /**
   * Default Gson instance
   *
   * @see #createGson(double, ExclusionStrategy...)
   * @see #getGson()
   */
  private static Gson gson;

  /** Contextual provided GSon adapters */
  private static Map<Class<?>, GsonAdapter<?>> gsonAdapaters;

  // GSON TYPES
  public static final Type TYPE_MAP = new TypeToken<Map<String, String>>() {
  }.getType();
  public static final Type TYPE_LIST = new TypeToken<List<String>>() {
  }.getType();

  private static final String JSON_NON_EXECUTABLE_PREFIX = ")]}'\n";

  /*
   * -- GSON CREATION --
   */

  /**
   * @return the default GSon instance
   * @see #createGson(double, ExclusionStrategy...)
   */
  public static Gson getGson() {
    // creates the default GSon instance on the first call or
    // if the application context has been initialized since
    // the first call (to retrieve the contextual GSon adapters)
    if (gson == null ||
        (gsonAdapaters == null))
      gson = createGson(0d);
    return gson;
  }
  public static Gson getGsonSerializeNulls() {
    return createGsonSerializeNulls(0d);
  }

  /**
   * This method registers the {@link GsonAdapter} found in the spring context thru the {@link GsonAdapterProvider} definition.
   *
   * @param version
   * @param exclusions
   * @return brand new {@link Gson} instance.
   */
  public static final Gson createGson(final double version, final ExclusionStrategy... exclusions) {
    return createGson(version, null, false, exclusions);
  }

  public static final Gson createGsonSerializeNulls(final double version, final ExclusionStrategy... exclusions) {
    return createGson(version, null, true, exclusions);
  }

  /**
   * This method registers the {@link GsonAdapter} found in the spring context thru the {@link GsonAdapterProvider} definition.
   *
   * @param version
   * @param typeAdaptors
   * @param exclusions
   * @return brand new {@link Gson} instance.
   */
  public static final Gson createGson(final double version, final Map<Object, Class<?>> typeAdaptors,boolean serializeNulls,
      final ExclusionStrategy... exclusions) {
    final GsonBuilder r = new GsonBuilder()
        // TODO sets the following commented parameters as method parameters
        // .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
        // .setPrettyPrinting()
        // .serializeNulls()
        .enableComplexMapKeySerialization()
        //.setDateFormat("MMM d, yyyy HH:mm:ss a")
        .setDateFormat(DEFAULT)
        .setVersion(version);
    if(serializeNulls) r.serializeNulls();

    // - ADAPTORS -
    // Defaults
    r.registerTypeAdapter(Class.class, ClassGsonAdapter.INSTANCE);
    r.registerTypeAdapter(byte[].class, ByteArrayAdapter.INSTANCE);
    r.registerTypeAdapter(LocalDateTime.class, LocalDateGsonAdapter.INSTANCE);
    // PropertyKey

    // Parameterized
    if (isNotEmpty(typeAdaptors))
      for (Entry<Object, Class<?>> e : typeAdaptors.entrySet())
      r.registerTypeAdapter(e.getValue(), e.getKey());
    // Provided (spring context)
    for (Entry<Class<?>, GsonAdapter<?>> e : getGsonAdapters().entrySet())
      r.registerTypeHierarchyAdapter(e.getKey(), e.getValue());

    // - EXCLUSIONS -
    // completes the given exclusions (if any)
    // with the default ones
    r.setExclusionStrategies(addAll(exclusions,
        TimeZoneExclusionStrategy.INSTANCE));

    return r.create();
  }

  private static final Map<Class<?>, GsonAdapter<?>> getGsonAdapters() {
    if (gsonAdapaters == null) {
      // looking for GsonAdapterProviders in the context
      GsonAdapterProvider provider;
      try {
        // single provider
        provider = SpringContextHelper.getBean(GsonAdapterProvider.class);
      }
      catch (NoUniqueBeanDefinitionException nubde) {
        log.debug("Error getting getGsonAdapters multi provider: {}", getRootCauseMessage(nubde));
        // multiple providers
        provider = new GsonAdapterProvider();
        final Map<String, GsonAdapterProvider> adapters = SpringContextHelper
            .getBeanOfType(GsonAdapterProvider.class);
        for (GsonAdapterProvider adapter : adapters.values())
          provider.getAdapters()
              .putAll(adapter.getAdapters());
      }
      catch (NoSuchBeanDefinitionException nsbde) {
        log.debug("Error getting getGsonAdapters no provider: {}", getRootCauseMessage(nsbde));
        // no provider
        provider = new GsonAdapterProvider();
      }
      gsonAdapaters = provider.getAdapters();
      log.debug("Provided GSon Adapters: {}", gsonAdapaters.toString());
    }
    return gsonAdapaters;
  }

  /*
   * PREDIFINED EXCLUSIONs
   */

  /** {@link TimeZone} exclusion */
  public static class TimeZoneExclusionStrategy implements ExclusionStrategy {
    public static final TimeZoneExclusionStrategy INSTANCE = new TimeZoneExclusionStrategy();

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return TimeZone.class.isAssignableFrom(f.getDeclaredClass());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return TimeZone.class.isAssignableFrom(clazz);
    }
  }

  public static class TaskPropertiesExclusionStrategy implements ExclusionStrategy {
    public static final TaskPropertiesExclusionStrategy INSTANCE = new TaskPropertiesExclusionStrategy();

    @Override
    public boolean shouldSkipField(FieldAttributes field) {
      return field.getAnnotation(Exclude.class) != null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  }

  /*
   * PREDIFINED ADAPTERs
   */

  /**
   * JSON de/serialize interface.
   *
   * @see JsonDeserializer
   * @see JsonSerializer
   * @param <T> serialized type
   */
  public interface GsonAdapter<T> extends JsonDeserializer<T>, JsonSerializer<T> {
    Class<T> getAdaptedClass();
  }

  /**
   * Base implementation for the adapter
   *
   * @param <T> the type supported by the adapter
   */
  public static abstract class AbstractGsonAdapter<T> implements GsonAdapter<T> {
    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getAdaptedClass() {
      final Type type = ((ParameterizedType) this.getClass()
          .getGenericSuperclass()).getActualTypeArguments()[0];

      // manage the parameterized entity type.
      if (type instanceof ParameterizedType)
        return (Class<T>) ((ParameterizedType) type).getRawType();

      return (Class<T>) type;
    }
  }

  public static class LocalDateGsonAdapter extends AbstractGsonAdapter<LocalDateTime> {
    public static final LocalDateGsonAdapter INSTANCE = new LocalDateGsonAdapter();

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
  }
  public static class LocaleGsonAdapter extends AbstractGsonAdapter<Locale> {
    public static final LocaleGsonAdapter INSTANCE = new LocaleGsonAdapter();

    @Override
    public JsonElement serialize(Locale src, Type type, JsonSerializationContext context) {
      return new JsonPrimitive(src.toString());
    }

    @Override
    public Locale deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      return new Locale(json.getAsString());
    }
  }

  /**
   * {@link Class} adapter.
   */
  public static class ClassGsonAdapter extends AbstractGsonAdapter<Class<?>> {
    public static final ClassGsonAdapter INSTANCE = new ClassGsonAdapter();

    @Override
    public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(src.getName());
    }

    @Override
    public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      try {
        return ClassUtils.getClass(json.getAsJsonPrimitive()
            .getAsString());
      }
      catch (ClassNotFoundException e) {
        throw new JsonParseException("Error parsing JSON Class<?> representation: " + json, e);
      }
    }
  };


  /**
   * {@link byte[]} adapter.
   */
  public static class ByteArrayAdapter extends AbstractGsonAdapter<byte[]> {
    public static final ByteArrayAdapter INSTANCE = new ByteArrayAdapter();
      public static String encodeToString(byte[] data) {
          // Encode the byte array to a Base64-encoded string
          String encodedString = Base64.getEncoder().encodeToString(data);
          return encodedString;
      }
    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(encodeToString(src));
    }

      public static byte[] decodeToByteArray(String encodedString) {
          // Decode the Base64-encoded string to a byte array
          byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
          return decodedBytes;
      }
    @Override
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      return decodeToByteArray(json.getAsString());
    }
  }

  /*
   * CONVERSION
   */

  public static final String fromMap(final Map<String, String> map) {
    return getGson().toJson(map, TYPE_MAP);
  }

  /** @return a {@link LinkedHashMap} from the given JSon (ie. preserve order) */
  public static final Map<String, String> toMap(final String json) {
    return getGson().fromJson(json, TYPE_MAP);
  }

  public static final String fromList(final List<String> list) {
    return getGson().toJson(list, TYPE_LIST);
  }

  public static final List<String> toList(final String json) {
    return getGson().fromJson(json, TYPE_LIST);
  }

  /**
   * @param generateNonExecutableJson Makes the output JSON non-executable in Javascript by prefixing the generated JSON with some special
   *          text. This prevents attacks from third-party sites through script sourcing. See
   *          <a href="http://code.google.com/p/google-gson/issues/detail?id=42">Gson Issue 42</a> for details.
   * @param prettyPrinting Allows Json output to fit in a page for pretty printing.
   * @param serializeNulls Omits or serializes all fields that are <code>null</code>.
   * @return a new JSON writer
   */
  public static final JsonWriter newJsonWriter(Writer writer,
      boolean generateNonExecutableJson, boolean prettyPrinting, boolean serializeNulls) throws IOException {
    if (generateNonExecutableJson)
      writer.write(JSON_NON_EXECUTABLE_PREFIX);

    final JsonWriter jsonWriter = new JsonWriter(writer);
    if (prettyPrinting)
      jsonWriter.setIndent("  ");

    jsonWriter.setSerializeNulls(serializeNulls);
    return jsonWriter;
  }

  /*
   * TYPE WRAPPING
   */

  private static final String JSON_TYPE_PROPERTY = "type";
  private static final String JSON_VALUE_PROPERTY = "value";

  // - TO JSON -
  public static final String toTypeWrappedJson(final Object object) {
    return toTypeWrappedJson(object, null, getGson());
  }

  public static final String toTypeWrappedJson(final Object object, final Class<?> type, final Gson gson) {
    if (object == null)
      return new String();
    final Class<?> clazz = type == null ? object.getClass() : type;
    final JsonElement jvalue;
    // - iterable -
    if (Iterable.class.isAssignableFrom(clazz)) {
      Iterable<?> iterable = (Iterable<?>) object;
      List<Object> list = new ArrayList<>();
      for (Object o : iterable)
        list.add(o);
      jvalue = new JsonPrimitive(toTypeWrappedJsonList(list));
    }
    // - array -
    else if (clazz.isArray()) {
      jvalue = new JsonPrimitive(toTypeWrappedJsonArray((Object[]) object));
    }
    // - simple -
    else
      jvalue = gson.toJsonTree(object, clazz);

    // adds class type to the JSon serialization
    final JsonObject jo = new JsonObject();
    jo.add(JSON_TYPE_PROPERTY, new JsonPrimitive(clazz.getName()));
    jo.add(JSON_VALUE_PROPERTY, jvalue);
    return jo.toString();
  }

  // - LIST TO JSON -
  public static final String toTypeWrappedJsonList(final Collection<?> objects) {
    return toTypeWrappedJsonList(objects, null, getGson());
  }

  public static final String toTypeWrappedJsonList(final Collection<?> objects, final Class<?> type, final Gson gson) {
    if (isEmpty(objects))
      return null; // NOSONAR : should be null
    final List<String> wraps = new ArrayList<>(objects.size());
    String wrap = null;
    for (Object object : objects) {
      wrap = toTypeWrappedJson(object, type, gson);
      if (isNotEmpty(wrap))
        wraps.add(wrap);
    }
    return fromList(wraps);
  }

  // - ARRAY TO JSON -
  public static final String toTypeWrappedJsonArray(final Object[] objects) {
    return toTypeWrappedJsonArray(objects, null, getGson());
  }

  public static final String toTypeWrappedJsonArray(final Object[] objects, final Class<?> type, final Gson gson) {
    return isEmpty(objects) ? null : toTypeWrappedJsonList(Arrays.asList(objects), type, gson);
  }

  // - FROM JSON -
  public static final <T> T fromTypeWrappedJson(final String json) {
    if (isBlank(json))
      return null;
    try (final StringReader reader = new StringReader(json)) {
      return fromTypeWrappedJson(reader, getGson());
    }
    catch (Exception e) {
      log.debug("Error getting fromTypeWrappedJson() : {}", getRootCauseMessage(e));
      log.error("Error reading Json: {} ({}) ({})", json, e.getMessage(), getRootCauseMessage(e));
      return null;
    }
  }

  public static final <T> T fromTypeWrappedJson(final Reader json, final Gson gson) {
    return fromTypeWrappedJson(gson.fromJson(json, JsonObject.class), gson);
  }

  @SuppressWarnings("unchecked")
  public static final <T> T fromTypeWrappedJson(final JsonObject jobject, final Gson gson) {
    final Class<T> jsonType;
    try {
      jsonType = (Class<T>) ClassUtils.getClass(jobject.get(JSON_TYPE_PROPERTY)
          .getAsJsonPrimitive()
          .getAsString());
    }
    catch (ClassNotFoundException e) {
      log.debug("Error getting fromTypeWrappedJson() : {}", getRootCauseMessage(e));
      throw new IllegalArgumentException("Impossible to retrieve the Json class type from: " + jobject);
    }

    JsonElement jvalue = jobject.get(JSON_VALUE_PROPERTY);
    // - iterable -
    // TODO manage jsonType (Set, List, Collection...)
    if (Iterable.class.isAssignableFrom(jsonType) && jvalue.isJsonPrimitive())
      return (T) fromTypeWrappedJsonList(jvalue.getAsJsonPrimitive()
          .getAsString());

    // - array -
    if (jsonType.isArray() && jvalue.isJsonPrimitive())
      return (T) fromTypeWrappedJsonArray(jvalue.getAsJsonPrimitive()
          .getAsString());

    // - simple -
    return gson.fromJson(jobject.get(JSON_VALUE_PROPERTY), jsonType);
  }

  // - FROM JSON LIST -
  public static final <T> List<T> fromTypeWrappedJsonList(final String json) {
    if (isBlank(json))
      return null; // NOSONAR : should be null
    try (final StringReader reader = new StringReader(json)) {
      return fromTypeWrappedJsonList(reader, getGson());
    }
    catch (Exception e) {
      log.error("Error reading Json: {} ({})", json, getRootCauseMessage(e));
      return null; // NOSONAR : should be null
    }
  }

  @SuppressWarnings("unchecked")
  public static final <T> List<T> fromTypeWrappedJsonList(final Reader json, final Gson gson) {
    final List<String> wraps = gson.fromJson(json, TYPE_LIST);
    final List<T> r = new ArrayList<>(wraps.size());
    for (String wrap : wraps)
      try (final StringReader reader = new StringReader(wrap)) {
        r.add((T) fromTypeWrappedJson(reader, gson));
      }
      catch (Exception e) {
        log.error("Error reading Json element: {} ({})", wrap, getRootCauseMessage(e));
      }
    return r;
  }

  public static final <T> T[] fromTypeWrappedJsonArray(final String json) {
    if (isBlank(json))
      return null; // NOSONAR : should be null
    try (final StringReader reader = new StringReader(json)) {
      return fromTypeWrappedJsonArray(reader, getGson());
    }
    catch (Exception e) {
      log.debug("Error getting fromTypeWrappedJsonArray() : {}", getRootCauseMessage(e));
      log.error("Error reading Json: {} ({})", json, e.getMessage());
      return null; // NOSONAR : should be null
    }
  }

  @SuppressWarnings("unchecked")
  public static final <T> T[] fromTypeWrappedJsonArray(final Reader json, final Gson gson) {
    final List<T> list = fromTypeWrappedJsonList(json, gson);
    if (isEmpty(list))
      return null; // NOSONAR : should be null
    return (T[]) list.toArray();
  }

  public static final boolean isWrapped(final JsonElement je) {
    return je != null && je.isJsonObject() && ((JsonObject) je).has(JSON_TYPE_PROPERTY);
  }

  /*
   * -- CLONING --
   */

  /**
   * @param object to JSon clone
   * @return the clone of the given object based on GSon serialization and de-serialization of the object's type.
   * @since 3.2
   */
  @SuppressWarnings("unchecked")
  public static final <T> T clone(final T object) throws CloneNotSupportedException {
    if (object == null)
      return null;
    try {
      return (T) getGson().fromJson(getGson().toJson(object), object.getClass());
    }
    catch (Exception e) {
      log.debug("Error getting clone() : {}", getRootCauseMessage(e));
      log.error("Error JSon cloning: {} ({})", object, e.getMessage());
      throw new CloneNotSupportedException("Error JSon cloning: " + object);
    }
  }

  /*
   * -- PRINTING --
   */

  /**
   * @param object the object to serialize to JSon
   * @return the JSon pretty string representation of the given object. If an error occurs during the JSon serialization, an empty string
   *         will be returned.
   */
  public static final String pretty(final Object object) {
    try (final StringWriter writer = new StringWriter()) {
      getGson().toJson(object, object.getClass(), newJsonWriter(writer, false, true, false));
      return writer.toString();
    }
    catch (Exception e) {
      log.debug("Error getting pretty() : {}", getRootCauseMessage(e));
      log.error("Error pretty printing: {} ({})", object, e.getMessage());
      return "";
    }
  }

  /*
   * -- CHECK --
   */

  /**
   * @param sequence to check
   * @return <code>true</code> if the given string can be parsed by the default {@link #getGson()} to a {@link JsonElement}. Note: if the
   *         given string is <code>null</code>, empty or a {@link JsonPrimitive}, <code>false</code> will be returned.
   */
  public static final boolean isJson(CharSequence sequence) {
    try {
      final JsonElement je = getGson().fromJson(sequence.toString(), JsonElement.class);
      return !je.isJsonPrimitive() && !je.isJsonNull();
    }
    catch (NullPointerException | JsonSyntaxException e) {
      log.trace("{} is not a valid JSon ({})", sequence, getRootCauseMessage(e));
      return false;
    }
  }

}
