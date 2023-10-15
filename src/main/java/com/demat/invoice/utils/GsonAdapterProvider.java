package com.demat.invoice.utils;

import com.demat.invoice.utils.GsonHelper.GsonAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Gson Adapter Provider used mainly in spring configuration to declare type conversion in Gson.
 * </p>
 * Example:
 *
 * <pre>
 * &lt;bean id="myGsonAdapter" class="com.byzaneo....MyGsonAdapter" /&gt;
 * &lt;bean class="com.byzaneo.commons.util.GsonAdapterProvider"&gt;
 *   &lt;property name="adapters"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="org...MyFirstInterface" value-ref="myGsonAdapter" /&gt;
 *       &lt;entry key="org...MyFirstImplementation" value-ref="myGsonAdapter" /&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author Romain Rossi <romain.rossi@byzaneo.com>
 * @company Byzaneo
 * @date May 22, 2013
 * @since 3.1
 */
public class GsonAdapterProvider {

  private Map<Class<?>, GsonAdapter<?>> adapters;

  public GsonAdapterProvider() {
    this.adapters = new HashMap<>();
  }

  public Map<Class<?>, GsonAdapter<?>> getAdapters() { // NOSONAR : generic wildcard types
    return adapters;
  }

  public void setAdapters(Map<Class<?>, GsonAdapter<?>> adapters) {
    this.adapters = adapters;
  }
}
