/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.capabilities;

import com.almende.eve.capabilities.handler.Handler;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Interface Capability.
 */
public interface Capability {
	
	/**
	 * Gets the resulting capability instance.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param <V>
	 *            the value type
	 * @param params
	 *            the params
	 * @param handle
	 *            the handle
	 * @param type
	 *            the type
	 * @return the t
	 */
	<T, V> T get(ObjectNode params, Handler<V> handle, Class<T> type);
}
