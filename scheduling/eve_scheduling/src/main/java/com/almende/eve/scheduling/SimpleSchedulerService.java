/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduling;

import java.util.HashMap;

import com.almende.eve.capabilities.handler.Handler;
import com.almende.eve.transport.Receiver;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SimpleSchedulerService.
 */
public class SimpleSchedulerService implements SchedulerService {
	private static final SimpleSchedulerService				singleton	= new SimpleSchedulerService();
	private static final TypeUtil<Handler<Receiver>>		TYPEUTIL	= new TypeUtil<Handler<Receiver>>() {
																		};
	private static final HashMap<String, SimpleScheduler>	instances	= new HashMap<String, SimpleScheduler>();
	
	/**
	 * Gets the instance by params.
	 * 
	 * @param params
	 *            the params
	 * @return the instance by params
	 */
	public static SimpleSchedulerService getInstanceByParams(
			final JsonNode params) {
		return singleton;
	}
	
	@Override
	public <T, V> T get(ObjectNode params, Handler<V> handle, Class<T> type) {
		SimpleScheduler result = null;
		if (handle.getKey() != null && instances.containsKey(handle.getKey())) {
			result = instances.get(handle.getKey());
			Handler<Receiver> oldHandle = result.getHandle();
			oldHandle.update(TYPEUTIL.inject(handle));
		} else {
			result = new SimpleScheduler(params, TYPEUTIL.inject(handle));
		}
		if (handle.getKey() != null) {
			instances.put(handle.getKey(), result);
		}
		return TypeUtil.inject(result, type);
	}
	
}
